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

package com.tencent.devops.log.resources

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.log.api.print.ServiceLogPrintResource
import com.tencent.devops.common.log.pojo.LogEvent
import com.tencent.devops.common.log.pojo.LogStatusEvent
import com.tencent.devops.common.log.pojo.message.LogMessage
import com.tencent.devops.log.service.BuildLogPrintService
import org.springframework.beans.factory.annotation.Autowired

/**
 *
 * Powered By Tencent
 */
@RestResource
class ServiceLogPrintResourceImpl @Autowired constructor(
    private val buildLogPrintService: BuildLogPrintService
) : ServiceLogPrintResource {

    override fun addLogLine(buildId: String, logMessage: LogMessage): Result<Boolean> {
        if (buildId.isBlank()) {
            throw ParamBlankException("无效的构建ID")
        }
        return buildLogPrintService.asyncDispatchEvent(LogEvent(buildId, listOf(logMessage)))
    }

    override fun addLogMultiLine(buildId: String, logMessages: List<LogMessage>): Result<Boolean> {
        if (buildId.isBlank()) {
            throw ParamBlankException("无效的构建ID")
        }
        buildLogPrintService.asyncDispatchEvent(LogEvent(buildId, logMessages))
        return Result(true)
    }

    override fun addLogStatus(
        buildId: String,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ): Result<Boolean> {
        if (buildId.isBlank()) {
            throw ParamBlankException("无效的构建ID")
        }
        return buildLogPrintService.asyncDispatchEvent(
            LogStatusEvent(
                buildId = buildId,
                finished = false,
                tag = tag ?: "",
                subTag = subTag,
                jobId = jobId ?: "",
                executeCount = executeCount
            )
        )
    }

    override fun updateLogStatus(
        buildId: String,
        finished: Boolean,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ): Result<Boolean> {
        if (buildId.isBlank()) {
            throw ParamBlankException("无效的构建ID")
        }
        return buildLogPrintService.asyncDispatchEvent(LogStatusEvent(
            buildId = buildId,
            finished = finished,
            tag = tag ?: "",
            subTag = subTag,
            jobId = jobId ?: "",
            executeCount = executeCount
        ))
    }
}