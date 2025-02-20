package com.tencent.devops.dockerhost.services.image

import com.tencent.devops.dockerhost.config.DockerHostConfig
import com.tencent.devops.dockerhost.dispatch.DockerHostBuildResourceApi
import com.tencent.devops.dockerhost.services.DockerHostImageScanService
import com.tencent.devops.dockerhost.services.Handler
import org.springframework.stereotype.Service

@Service
class ImageScanHandler(
    private val dockerHostImageScanService: DockerHostImageScanService,
    dockerHostConfig: DockerHostConfig,
    dockerHostBuildApi: DockerHostBuildResourceApi
) : Handler<ImageHandlerContext>(dockerHostConfig, dockerHostBuildApi) {
    override fun handlerRequest(handlerContext: ImageHandlerContext) {
        with(handlerContext) {
            if (scanFlag) {
                dockerHostImageScanService.scanningDocker(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    vmSeqId = vmSeqId.toString(),
                    userName = userName,
                    imageTagSet = imageTagSet,
                    dockerClient = dockerClient
                )
            }

            nextHandler.get()?.handlerRequest(this)
        }
    }
}
