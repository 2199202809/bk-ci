/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.engine.control.command.stage.impl

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.engine.common.BS_MANUAL_START_STAGE
import com.tencent.devops.process.engine.control.command.CmdFlowState
import com.tencent.devops.process.engine.control.command.stage.StageCmd
import com.tencent.devops.process.engine.control.command.stage.StageContext
import com.tencent.devops.process.engine.pojo.PipelineBuildStage
import com.tencent.devops.process.engine.pojo.event.PipelineBuildNotifyEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStageEvent
import com.tencent.devops.process.pojo.PipelineNotifyTemplateEnum
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_NAME
import com.tencent.devops.quality.api.v2.pojo.ControlPointPosition
import com.tencent.devops.quality.api.v3.ServiceQualityRuleResource
import com.tencent.devops.quality.api.v3.pojo.request.BuildCheckParamsV3
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Date

/**
 * Stage暂停及审核事件的命令处理
 */
@Service
class CheckPauseReviewStageCmd(
    private val buildVariableService: BuildVariableService,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val client: Client
) : StageCmd {

    override fun canExecute(commandContext: StageContext): Boolean {
        return commandContext.stage.controlOption?.finally != true &&
            commandContext.cmdFlowState == CmdFlowState.CONTINUE &&
            !commandContext.buildStatus.isFinish()
    }

    override fun execute(commandContext: StageContext) {
        val stage = commandContext.stage
        val event = commandContext.event

        // #115 若stage状态为暂停，且来源不是BS_MANUAL_START_STAGE，碰到状态为暂停就停止运行
        if (commandContext.buildStatus.isPause() && event.source != BS_MANUAL_START_STAGE) {

            LOG.info("ENGINE|${event.buildId}|${event.source}|STAGE_STOP_BY_PAUSE|${event.stageId}")
            commandContext.latestSummary = "s(${stage.stageId}) already in PAUSE!"
            commandContext.cmdFlowState = CmdFlowState.BREAK
        } else if (commandContext.buildStatus.isReadyToRun()) {

            // 质量红线
            if (checkQualityFailed(event, stage)) {
                // #4732 优先判断是否能通过质量红线检查
                LOG.info("ENGINE|${event.buildId}|${event.source}|STAGE_QUALITY_CHECK_FAILED|${event.stageId}")
                // TODO 暂时只处理准入，后续需要兼容准出
                commandContext.stage.checkIn?.status = BuildStatus.QUALITY_CHECK_FAIL.name
                commandContext.buildStatus = BuildStatus.QUALITY_CHECK_FAIL
                commandContext.latestSummary = "s(${stage.stageId}) failed with QUALITY_CHECK_IN"
                commandContext.cmdFlowState = CmdFlowState.CONTINUE
                return
            }

            // 人工审核
            if (needPause(event, stage)) {
                // #3742 进入暂停状态则刷新完状态后直接返回，等待手动触发
                LOG.info("ENGINE|${event.buildId}|${event.source}|STAGE_PAUSE|${event.stageId}")
                pauseStageNotify(commandContext)
                commandContext.buildStatus = BuildStatus.STAGE_SUCCESS
                commandContext.latestSummary = "s(${stage.stageId}) waiting for REVIEW"
                commandContext.cmdFlowState = CmdFlowState.FINALLY
                return
            }

            commandContext.cmdFlowState = CmdFlowState.CONTINUE
            // #3742 只有经过手动审核才做审核变量的保存
            if (stage.checkIn?.manualTrigger == true) {
                saveStageReviewParams(stage = stage)
            }
        }
    }

    private fun saveStageReviewParams(stage: PipelineBuildStage) {
        val reviewVariables = mutableMapOf<String, String>()
        // # 4531 遍历全部审核组的参数，后序覆盖前序的同名变量
        stage.checkIn?.reviewGroups?.forEach { group ->
            group.params?.forEach {
                reviewVariables[it.key] = it.value.toString()
            }
        }
        if (stage.checkIn?.reviewParams?.isNotEmpty() == true) {
            buildVariableService.batchUpdateVariable(
                projectId = stage.projectId,
                pipelineId = stage.pipelineId,
                buildId = stage.buildId,
                variables = reviewVariables
            )
        }
    }

    private fun checkQualityFailed(event: PipelineBuildStageEvent, stage: PipelineBuildStage): Boolean {
        if (stage.checkIn?.ruleIds.isNullOrEmpty()) return false
        try {
            val result = client.get(ServiceQualityRuleResource::class).check(BuildCheckParamsV3(
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                buildId = event.buildId,
                position = ControlPointPosition.BEFORE_POSITION,
                templateId = null,
                interceptName = null,
                ruleBuildIds = stage.checkIn?.ruleIds!!.toSet(),
                runtimeVariable = null
            )).data!!
            LOG.info("ENGINE|${event.buildId}|${event.source}|STAGE_QUALITY_CHECK|${event.stageId}|" +
                "result=$result|ruleIds=${stage.checkIn?.ruleIds}")
            return !result.success
        } catch (ignore: Throwable) {
            LOG.error("ENGINE|${event.buildId}|${event.source}|STAGE_QUALITY_CHECK_ERROR|${event.stageId}", ignore)
            return true
        }
    }

    private fun needPause(event: PipelineBuildStageEvent, stage: PipelineBuildStage): Boolean {
        // #115 只有在非手动触发该Stage的首次运行做审核暂停
        if (event.source == BS_MANUAL_START_STAGE || stage.checkIn?.manualTrigger != true) {
            return false
        }
        // TODO 下次发布去掉对triggered的判断
        return stage.checkIn?.groupToReview() != null ||
            stage.controlOption?.stageControlOption?.triggered != true
    }

    /**
     * 发送通知
     */
    private fun pauseStageNotify(commandContext: StageContext) {
        val stage = commandContext.stage
        val checkIn = stage.checkIn ?: return
        val group = stage.checkIn?.groupToReview() ?: return
        val notifyUsers = mutableListOf<String>()

        if (group.status == null) {
            val reviewers = group.reviewers.joinToString(",")
            val realReviewers = EnvUtils.parseEnv(reviewers, commandContext.variables)
                .split(",").toList()
            group.reviewers = realReviewers
            notifyUsers.addAll(realReviewers)
        }

        val pipelineName = commandContext.variables[PIPELINE_NAME] ?: stage.pipelineId
        val buildNum = commandContext.variables[PIPELINE_BUILD_NUM] ?: "1"
        var reviewDesc = checkIn.reviewDesc
        reviewDesc = EnvUtils.parseEnv(reviewDesc, commandContext.variables)
        checkIn.reviewDesc = reviewDesc // 替换变量 #3395

        pipelineEventDispatcher.dispatch(
            PipelineBuildNotifyEvent(
                notifyTemplateEnum = PipelineNotifyTemplateEnum.PIPELINE_MANUAL_REVIEW_STAGE_NOTIFY_TEMPLATE.name,
                source = commandContext.latestSummary, projectId = stage.projectId, pipelineId = stage.pipelineId,
                userId = commandContext.event.userId, buildId = stage.buildId,
                receivers = notifyUsers,
                titleParams = mutableMapOf(
                    "projectName" to "need to add in notifyListener",
                    "pipelineName" to pipelineName,
                    "buildNum" to buildNum
                ),
                bodyParams = mutableMapOf(
                    "projectName" to "need to add in notifyListener",
                    "pipelineName" to pipelineName,
                    "dataTime" to DateTimeUtil.formatDate(Date(), "yyyy-MM-dd HH:mm:ss"),
                    "reviewDesc" to reviewDesc
                )
            )
        )
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(CheckPauseReviewStageCmd::class.java)
    }
}
