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

package com.tencent.devops.common.webhook.service.code.handler.tgit

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.webhook.annotation.CodeWebhookHandler
import com.tencent.devops.common.webhook.pojo.code.PathFilterConfig
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.common.webhook.service.code.GitScmService
import com.tencent.devops.common.webhook.service.code.filter.BranchFilter
import com.tencent.devops.common.webhook.service.code.filter.PathFilterFactory
import com.tencent.devops.common.webhook.service.code.filter.SkipCiFilter
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilter
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilterResponse
import com.tencent.devops.common.webhook.service.code.handler.GitHookTriggerHandler
import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.common.webhook.util.WebhookUtils.convert
import com.tencent.devops.common.webhook.util.WebhookUtils.getBranch
import com.tencent.devops.process.engine.service.code.filter.CommitMessageFilter
import com.tencent.devops.repository.pojo.CodeGitlabRepository
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.scm.pojo.BK_REPO_GIT_MANUAL_UNLOCK
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_ASSIGNEE
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_AUTHOR
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_BASE_COMMIT
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_CREATE_TIME
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_CREATE_TIMESTAMP
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_DESCRIPTION
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_ID
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_LABELS
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_LAST_COMMIT
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_LAST_COMMIT_MSG
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_MERGE_COMMIT_SHA
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_MERGE_TYPE
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_MILESTONE
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_MILESTONE_DUE_DATE
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_NUMBER
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_REVIEWERS
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_SOURCE_BRANCH
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_SOURCE_COMMIT
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_SOURCE_URL
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_TARGET_BRANCH
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_TARGET_COMMIT
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_TARGET_URL
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_TITLE
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIME
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIMESTAMP
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_URL
import com.tencent.devops.scm.pojo.GIT_MR_NUMBER
import com.tencent.devops.scm.pojo.PIPELINE_WEBHOOK_MR_COMMITTER
import com.tencent.devops.scm.pojo.PIPELINE_WEBHOOK_MR_ID
import com.tencent.devops.scm.pojo.PIPELINE_WEBHOOK_SOURCE_BRANCH
import com.tencent.devops.scm.pojo.PIPELINE_WEBHOOK_SOURCE_PROJECT_ID
import com.tencent.devops.scm.pojo.PIPELINE_WEBHOOK_SOURCE_REPO_NAME
import com.tencent.devops.scm.pojo.PIPELINE_WEBHOOK_TARGET_BRANCH
import com.tencent.devops.scm.pojo.PIPELINE_WEBHOOK_TARGET_PROJECT_ID
import com.tencent.devops.scm.pojo.PIPELINE_WEBHOOK_TARGET_REPO_NAME
import com.tencent.devops.scm.utils.code.git.GitUtils
import org.slf4j.LoggerFactory

@CodeWebhookHandler
@Suppress("TooManyFunctions")
class TGitMrTriggerHandler(
    private val gitScmService: GitScmService
) : GitHookTriggerHandler<GitMergeRequestEvent> {

    companion object {
        private val logger = LoggerFactory.getLogger(TGitMrTriggerHandler::class.java)
    }

    override fun eventClass(): Class<GitMergeRequestEvent> {
        return GitMergeRequestEvent::class.java
    }

    override fun getEventType(): CodeEventType {
        return CodeEventType.MERGE_REQUEST
    }

    override fun getUrl(event: GitMergeRequestEvent): String {
        return event.object_attributes.target.http_url
    }

    override fun getUsername(event: GitMergeRequestEvent): String {
        return event.user.username
    }

    override fun getAction(event: GitMergeRequestEvent): String? {
        return event.object_attributes.action
    }

    override fun getRevision(event: GitMergeRequestEvent): String {
        return event.object_attributes.last_commit.id
    }

    override fun getRepoName(event: GitMergeRequestEvent): String {
        return GitUtils.getProjectName(event.object_attributes.target.ssh_url)
    }

    override fun getBranchName(event: GitMergeRequestEvent): String {
        return event.object_attributes.target_branch
    }

    override fun getMessage(event: GitMergeRequestEvent): String {
        return event.object_attributes.last_commit.message
    }

    override fun getHookSourceUrl(event: GitMergeRequestEvent): String {
        return event.object_attributes.source.http_url
    }

    override fun getHookTargetUrl(event: GitMergeRequestEvent): String {
        return event.object_attributes.target.http_url
    }

    override fun getMergeRequestId(event: GitMergeRequestEvent): Long {
        return event.object_attributes.id
    }

    override fun getEnv(event: GitMergeRequestEvent): Map<String, Any> {
        return mapOf(
            GIT_MR_NUMBER to event.object_attributes.iid,
            BK_REPO_GIT_MANUAL_UNLOCK to (event.manual_unlock ?: false)
        )
    }

    override fun preMatch(event: GitMergeRequestEvent): ScmWebhookMatcher.MatchResult {
        if (event.object_attributes.action == "close" ||
            (event.object_attributes.action == "update" &&
                event.object_attributes.extension_action != "push-update")
        ) {
            logger.info("Git web hook is ${event.object_attributes.action} merge request")
            return ScmWebhookMatcher.MatchResult(false)
        }
        return ScmWebhookMatcher.MatchResult(true)
    }

    override fun getEventFilters(
        event: GitMergeRequestEvent,
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: WebHookParams
    ): List<WebhookFilter> {
        with(webHookParams) {
            val sourceBranchFilter = BranchFilter(
                pipelineId = pipelineId,
                triggerOnBranchName = getBranch(event.object_attributes.source_branch),
                includedBranches = convert(includeSourceBranchName),
                excludedBranches = convert(excludeSourceBranchName)
            )
            val skipCiFilter = SkipCiFilter(
                pipelineId = pipelineId,
                triggerOnMessage = event.object_attributes.last_commit.message
            )

            // 懒加载请求修改的路径,只有前面所有匹配通过,再去查询
            val pathFilter = object : WebhookFilter {
                override fun doFilter(response: WebhookFilterResponse): Boolean {
                    // 只有开启路径匹配时才查询mr change file list
                    val mrChangeInfo = if (excludePaths.isNullOrBlank() && includePaths.isNullOrBlank()) {
                        null
                    } else {
                        val mrId = if (repository is CodeGitlabRepository) {
                            event.object_attributes.iid
                        } else {
                            event.object_attributes.id
                        }
                        gitScmService.getMergeRequestChangeInfo(projectId, mrId, repository)
                    }
                    val changeFiles = mrChangeInfo?.files?.map {
                        if (it.deletedFile) {
                            it.oldPath
                        } else {
                            it.newPath
                        }
                    } ?: emptyList()
                    return PathFilterFactory.newPathFilter(
                        PathFilterConfig(
                            pathFilterType = pathFilterType,
                            pipelineId = pipelineId,
                            triggerOnPath = changeFiles,
                            includedPaths = convert(includePaths),
                            excludedPaths = convert(excludePaths)
                        )
                    ).doFilter(response)
                }
            }
            val commitMessageFilter = CommitMessageFilter(
                includeCommitMsg,
                excludeCommitMsg,
                event.object_attributes.last_commit.message,
                pipelineId
            )
            return listOf(sourceBranchFilter, skipCiFilter, pathFilter, commitMessageFilter)
        }
    }

    override fun retrieveParams(
        event: GitMergeRequestEvent,
        projectId: String?,
        repository: Repository?
    ): Map<String, Any> {
        val startParams = mutableMapOf<String, Any>()
        startParams.putAll(
            mrStartParam(
                event = event,
                projectId = projectId,
                repository = repository
            )
        )
        startParams[PIPELINE_WEBHOOK_SOURCE_BRANCH] = event.object_attributes.source_branch
        startParams[PIPELINE_WEBHOOK_TARGET_BRANCH] = event.object_attributes.target_branch
        startParams[BK_REPO_GIT_WEBHOOK_MR_TARGET_BRANCH] = event.object_attributes.target_branch
        startParams[BK_REPO_GIT_WEBHOOK_MR_SOURCE_BRANCH] = event.object_attributes.source_branch
        startParams[PIPELINE_WEBHOOK_SOURCE_PROJECT_ID] = event.object_attributes.source_project_id
        startParams[PIPELINE_WEBHOOK_TARGET_PROJECT_ID] = event.object_attributes.target_project_id
        startParams[PIPELINE_WEBHOOK_SOURCE_REPO_NAME] =
            GitUtils.getProjectName(event.object_attributes.source.http_url)
        startParams[PIPELINE_WEBHOOK_TARGET_REPO_NAME] =
            GitUtils.getProjectName(event.object_attributes.target.http_url)
        startParams[BK_REPO_GIT_WEBHOOK_MR_URL] = event.object_attributes.url
        val lastCommit = event.object_attributes.last_commit
        startParams[BK_REPO_GIT_WEBHOOK_MR_LAST_COMMIT] = lastCommit.id
        startParams[BK_REPO_GIT_WEBHOOK_MR_LAST_COMMIT_MSG] = lastCommit.message
        return startParams
    }

    @SuppressWarnings("ComplexMethod")
    private fun mrStartParam(
        event: GitMergeRequestEvent,
        projectId: String?,
        repository: Repository?
    ): Map<String, Any> {
        if (projectId == null || repository == null) {
            return emptyMap()
        }
        val startParams = mutableMapOf<String, Any>()
        val mrRequestId = if (repository is CodeGitlabRepository) {
            event.object_attributes.iid
        } else {
            event.object_attributes.id
        }
        // MR提交人
        val mrInfo = gitScmService.getMergeRequestInfo(projectId, mrRequestId, repository)
        val reviewers = gitScmService.getMergeRequestReviewersInfo(projectId, mrRequestId, repository)?.reviewers

        startParams[PIPELINE_WEBHOOK_MR_ID] = mrRequestId
        startParams[PIPELINE_WEBHOOK_MR_COMMITTER] = mrInfo?.author?.username ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_AUTHOR] = mrInfo?.author?.username ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_TARGET_URL] = event.object_attributes.target.http_url
        startParams[BK_REPO_GIT_WEBHOOK_MR_SOURCE_URL] = event.object_attributes.source.http_url
        startParams[BK_REPO_GIT_WEBHOOK_MR_CREATE_TIME] = mrInfo?.createTime ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIME] = mrInfo?.updateTime ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_CREATE_TIMESTAMP] = DateTimeUtil.zoneDateToTimestamp(mrInfo?.createTime)
        startParams[BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIMESTAMP] = DateTimeUtil.zoneDateToTimestamp(mrInfo?.updateTime)
        startParams[BK_REPO_GIT_WEBHOOK_MR_ID] = mrInfo?.mrId ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_NUMBER] = mrInfo?.mrNumber ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_DESCRIPTION] = mrInfo?.description ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_TITLE] = mrInfo?.title ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_ASSIGNEE] = mrInfo?.assignee?.username ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_REVIEWERS] = reviewers?.joinToString(",") { it.username } ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_MILESTONE] = mrInfo?.milestone?.title ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_MILESTONE_DUE_DATE] = mrInfo?.milestone?.dueDate ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_LABELS] = mrInfo?.labels?.joinToString(",") ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_MERGE_TYPE] = event.object_attributes.mergeType ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_MERGE_COMMIT_SHA] = event.object_attributes.mergeCommitSha ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_BASE_COMMIT] = mrInfo?.baseCommit ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_TARGET_COMMIT] = mrInfo?.targetCommit ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_SOURCE_COMMIT] = mrInfo?.sourceCommit ?: ""
        return startParams
    }
}
