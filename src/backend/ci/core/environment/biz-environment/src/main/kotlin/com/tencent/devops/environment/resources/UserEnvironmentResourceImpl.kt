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

package com.tencent.devops.environment.resources

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.UserEnvironmentResource
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.EnvCreateInfo
import com.tencent.devops.environment.pojo.EnvUpdateInfo
import com.tencent.devops.environment.pojo.EnvWithNodeCount
import com.tencent.devops.environment.pojo.EnvWithPermission
import com.tencent.devops.environment.pojo.EnvironmentId
import com.tencent.devops.environment.pojo.NodeBaseInfo
import com.tencent.devops.environment.pojo.SharedProjectInfo
import com.tencent.devops.environment.pojo.SharedProjectInfoWrap
import com.tencent.devops.environment.pojo.enums.EnvType
import com.tencent.devops.environment.service.EnvService
import org.springframework.beans.factory.annotation.Autowired

@Suppress("ALL")
@RestResource
class UserEnvironmentResourceImpl @Autowired constructor(
    private val envService: EnvService,
    private val environmentPermissionService: EnvironmentPermissionService
) : UserEnvironmentResource {
    override fun listUsableServerEnvs(userId: String, projectId: String): Result<List<EnvWithPermission>> {
        return Result(envService.listUsableServerEnvs(userId, projectId))
    }

    override fun hasCreatePermission(userId: String, projectId: String): Result<Boolean> {
        return Result(environmentPermissionService.checkEnvPermission(userId, projectId, AuthPermission.CREATE))
    }

    override fun create(userId: String, projectId: String, environment: EnvCreateInfo): Result<EnvironmentId> {
        if (environment.name.isBlank()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_NAME_NULL)
        }
        if (environment.name.isBlank()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_NAME_TOO_LONG)
        }

        return Result(envService.createEnvironment(userId, projectId, environment))
    }

    override fun update(
        userId: String,
        projectId: String,
        envHashId: String,
        environment: EnvUpdateInfo
    ): Result<Boolean> {
        if (envHashId.isBlank()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_ID_NULL)
        }

        if (environment.name.isBlank()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_NAME_NULL)
        }

        envService.updateEnvironment(userId, projectId, envHashId, environment)
        return Result(true)
    }

    override fun list(userId: String, projectId: String): Result<List<EnvWithPermission>> {
        return Result(envService.listEnvironment(userId, projectId))
    }

    override fun listByType(userId: String, projectId: String, envType: EnvType): Result<List<EnvWithNodeCount>> {
        return Result(envService.listEnvironmentByType(userId, projectId, envType))
    }

    override fun listBuildEnvs(userId: String, projectId: String, os: OS): Result<List<EnvWithNodeCount>> {
        return Result(envService.listBuildEnvs(userId, projectId, os))
    }

    override fun get(userId: String, projectId: String, envHashId: String): Result<EnvWithPermission> {
        if (envHashId.isBlank()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_ID_NULL)
        }

        return Result(envService.getEnvironment(userId, projectId, envHashId))
    }

    override fun delete(userId: String, projectId: String, envHashId: String): Result<Boolean> {
        if (envHashId.isBlank()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_ID_NULL)
        }

        envService.deleteEnvironment(userId, projectId, envHashId)
        return Result(true)
    }

    override fun listNodes(userId: String, projectId: String, envHashId: String): Result<List<NodeBaseInfo>> {
        if (envHashId.isBlank()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_ID_NULL)
        }

        return Result(envService.listAllEnvNodes(userId, projectId, listOf(envHashId)))
    }

    override fun addNodes(
        userId: String,
        projectId: String,
        envHashId: String,
        nodeHashIds: List<String>
    ): Result<Boolean> {
        if (envHashId.isBlank()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_ID_NULL)
        }

        if (nodeHashIds.isEmpty()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_NODE_HASH_ID_ILLEGAL)
        }

        envService.addEnvNodes(userId, projectId, envHashId, nodeHashIds)
        return Result(true)
    }

    override fun deleteNodes(
        userId: String,
        projectId: String,
        envHashId: String,
        nodeHashIds: List<String>
    ): Result<Boolean> {
        if (envHashId.isBlank()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_ID_NULL)
        }

        if (nodeHashIds.isEmpty()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_NODE_HASH_ID_ILLEGAL)
        }

        envService.deleteEnvNodes(userId, projectId, envHashId, nodeHashIds)
        return Result(true)
    }

    override fun listShareEnv(
        userId: String,
        projectId: String,
        envHashId: String,
        name: String?,
        offset: Int?,
        limit: Int?
    ): Result<Page<SharedProjectInfo>> {
        checkParam(userId, projectId, envHashId)
        return Result(envService.listShareEnv(
            userId,
            projectId,
            envHashId,
            name,
            offset ?: 0,
            limit ?: 20
        ))
    }

    override fun setShareEnv(
        userId: String,
        projectId: String,
        envHashId: String,
        sharedProjects: SharedProjectInfoWrap
    ): Result<Boolean> {
        checkParam(userId, projectId, envHashId)
        envService.setShareEnv(userId, projectId, envHashId, sharedProjects.sharedProjects)
        return Result(true)
    }

    override fun deleteShareEnv(userId: String, projectId: String, envHashId: String): Result<Boolean> {
        checkParam(userId, projectId, envHashId)
        envService.deleteShareEnv(userId, projectId, envHashId)
        return Result(true)
    }

    override fun deleteShareEnvBySharedProj(
        userId: String,
        projectId: String,
        envHashId: String,
        sharedProjectId: String
    ): Result<Boolean> {
        checkParam(userId, projectId, envHashId)
        envService.deleteShareEnvBySharedProj(userId, projectId, envHashId, sharedProjectId)
        return Result(true)
    }

    private fun checkParam(
        userId: String,
        projectId: String,
        envHashId: String
    ) {
        if (userId.isBlank()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_ID_NULL)
        }

        if (envHashId.isBlank()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_ID_NULL)
        }

        if (projectId.isEmpty()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_NODE_SHARE_PROJECT_EMPTY)
        }
    }
}
