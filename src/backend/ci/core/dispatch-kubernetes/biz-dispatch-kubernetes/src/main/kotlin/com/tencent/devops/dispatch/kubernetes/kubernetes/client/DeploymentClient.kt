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

package com.tencent.devops.dispatch.kubernetes.kubernetes.client

import com.google.gson.Gson
import com.tencent.devops.dispatch.kubernetes.config.DispatchBuildConfig
import com.tencent.devops.dispatch.kubernetes.config.KubernetesClientConfig
import com.tencent.devops.dispatch.kubernetes.kubernetes.model.deployment.Deployment
import com.tencent.devops.dispatch.kubernetes.kubernetes.model.deployment.DeploymentData
import com.tencent.devops.dispatch.kubernetes.kubernetes.model.pod.ConfigMap
import com.tencent.devops.dispatch.kubernetes.kubernetes.model.pod.ContainerData
import com.tencent.devops.dispatch.kubernetes.kubernetes.model.pod.PodData
import com.tencent.devops.dispatch.kubernetes.kubernetes.model.pod.Volume
import com.tencent.devops.dispatch.kubernetes.kubernetes.model.pod.VolumeMount
import com.tencent.devops.dispatch.kubernetes.pojo.BuildContainer
import com.tencent.devops.dispatch.kubernetes.pojo.Params
import com.tencent.devops.dispatch.kubernetes.utils.KubernetesClientUtil.toLabelSelector
import io.kubernetes.client.custom.V1Patch
import io.kubernetes.client.openapi.ApiResponse
import io.kubernetes.client.openapi.apis.AppsV1Api
import io.kubernetes.client.openapi.models.V1Deployment
import io.kubernetes.client.openapi.models.V1DeploymentList
import io.kubernetes.client.openapi.models.V1Status
import io.kubernetes.client.util.PatchUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class DeploymentClient @Autowired constructor(
    private val k8sConfig: KubernetesClientConfig,
    private val dispatchBuildConfig: DispatchBuildConfig
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DeploymentClient::class.java)
    }

    private val volumeName = "config"

    fun create(
        buildContainer: BuildContainer,
        containerName: String
    ): ApiResponse<V1Deployment> {
        val labels = getCoreLabels(containerName)

        logger.info("DeploymentClient create containerName: $containerName buildContainer: $buildContainer")

        val deployment = with(buildContainer) {
            Deployment.deployment(
                DeploymentData(
                    apiVersion = "apps/v1",
                    name = containerName,
                    nameSpace = k8sConfig.nameSpace!!,
                    replicas = 1,
                    labels = labels,
                    selectorLabels = labels,
                    pod = PodData(
                        labels = labels,
                        container = ContainerData(
                            imageName = containerName,
                            image = image,
                            cpu = cpu.toString(),
                            memory = memory,
                            disk = disk,
                            ports = ports,
                            env = params?.env,
                            commends = params?.command,
                            volumeMounts = listOf(
                                VolumeMount(
                                    name = volumeName,
                                    mountPath = dispatchBuildConfig.volumeMountPath!!
                                )
                            )
                        ),
                        volume = Volume(
                            name = volumeName,
                            configMap = ConfigMap(
                                name = k8sConfig.configMapName!!,
                                key = dispatchBuildConfig.volumeConfigMapKey!!,
                                path = dispatchBuildConfig.volumeConfigMapPath!!
                            )
                        )
                    )
                )
            )
        }
        return AppsV1Api().createNamespacedDeploymentWithHttpInfo(
            k8sConfig.nameSpace, deployment, null, null, null
        )
    }

    fun delete(
        containerName: String
    ): ApiResponse<V1Status> {
        return AppsV1Api().deleteNamespacedDeploymentWithHttpInfo(
            containerName,
            k8sConfig.nameSpace,
            null, null, null, null, null, null
        )
    }

    // 目前启动，和停止通过扩，缩容deployment的pod数量实现
    fun start(
        containerName: String,
        params: Params?
    ): V1Deployment {
        val labels = getCoreLabels(containerName)
        val deployment = Gson().toJson(
            Deployment.deployment(
                DeploymentData(
                    apiVersion = "apps/v1",
                    name = containerName,
                    nameSpace = k8sConfig.nameSpace!!,
                    replicas = 1,
                    labels = labels,
                    selectorLabels = labels,
                    pod = PodData(
                        labels = labels,
                        container = ContainerData(
                            imageName = containerName,
                            image = null,
                            cpu = null,
                            memory = null,
                            disk = null,
                            ports = null,
                            env = params?.env,
                            commends = params?.command,
                            volumeMounts = listOf(
                                VolumeMount(
                                    name = volumeName,
                                    mountPath = dispatchBuildConfig.volumeMountPath!!
                                )
                            )
                        ),
                        volume = Volume(
                            name = volumeName,
                            configMap = ConfigMap(
                                name = k8sConfig.configMapName!!,
                                key = dispatchBuildConfig.volumeConfigMapKey!!,
                                path = dispatchBuildConfig.volumeConfigMapPath!!
                            )
                        )
                    )
                )
            )
        )
        val appsApi = AppsV1Api()
        return PatchUtils.patch(
            V1Deployment::class.java,
            {
                appsApi.patchNamespacedDeploymentCall(
                    containerName,
                    k8sConfig.nameSpace,
                    V1Patch(deployment),
                    null,
                    null,
                    "start $containerName", // field-manager is required for server-side apply
                    true,
                    null
                )
            },
            V1Patch.PATCH_FORMAT_APPLY_YAML,
            appsApi.apiClient
        )
    }

    fun stop(
        containerName: String
    ): ApiResponse<V1Deployment> {
        val stopJson = Gson().toJson(
            listOf(
                mapOf(
                    "op" to "replace",
                    "path" to "/spec/replicas",
                    "value" to 0
                )
            )
        )
        return AppsV1Api().patchNamespacedDeploymentWithHttpInfo(
            containerName,
            k8sConfig.nameSpace,
            V1Patch(stopJson),
            null, null, null, null
        )
    }

    fun list(
        containerName: String
    ): ApiResponse<V1DeploymentList> {
        return AppsV1Api().listNamespacedDeploymentWithHttpInfo(
            k8sConfig.nameSpace,
            "true",
            null,
            null,
            null,
            mapOf(dispatchBuildConfig.label!! to containerName).toLabelSelector(),
            null, null, null, null, null
        )
    }

    private fun getCoreLabels(containerName: String) = mapOf("container-name" to containerName)
}
