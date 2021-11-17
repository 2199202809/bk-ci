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

import com.tencent.devops.dispatch.kubernetes.config.DispatchBuildConfig
import com.tencent.devops.dispatch.kubernetes.config.KubernetesClientConfig
import com.tencent.devops.dispatch.kubernetes.utils.KubernetesClientUtil.toLabelSelector
import io.kubernetes.client.openapi.ApiResponse
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.models.V1Pod
import io.kubernetes.client.openapi.models.V1PodList
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class PodsClient @Autowired constructor(
    private val k8sConfig: KubernetesClientConfig,
    private val dispatchBuildConfig: DispatchBuildConfig
) {

    fun listWithHttpInfo(
        name: String
    ): ApiResponse<V1PodList> {
        return CoreV1Api().listNamespacedPodWithHttpInfo(
            k8sConfig.nameSpace,
            "true",
            null,
            null,
            null,
            mapOf(dispatchBuildConfig.containerLabel!! to name).toLabelSelector(),
            null, null, null, null, null
        )
    }

    fun list(
        name: String
    ): V1PodList {
        return CoreV1Api().listNamespacedPod(
            k8sConfig.nameSpace,
            "true",
            null,
            null,
            null,
            mapOf(dispatchBuildConfig.containerLabel!! to name).toLabelSelector(),
            null, null, null, null, null
        )
    }


    fun read(
        podName: String
    ): V1Pod? {
        return try {
            CoreV1Api().readNamespacedPod(
                podName,
                k8sConfig.nameSpace,
                "true",
                null,
                null
            )
        } catch (ignore: Exception) {
            return null
        }
    }
}
