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

package com.tencent.devops.process.engine.atom.task

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ManualReviewAction
import com.tencent.devops.common.pipeline.pojo.element.agent.ManualReviewUserTaskElement
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION_PARAMS
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION_SUGGEST
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION_USERID
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.event.PipelineBuildNotifyEvent
import com.tencent.devops.process.pojo.PipelineNotifyTemplateEnum
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_NAME
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.Date

/**
 * 人工审核插件
 */
@Suppress("UNUSED")
@Component
class ManualReviewTaskAtom(
    private val buildLogPrinter: BuildLogPrinter,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val pipelineVariableService: BuildVariableService
) : IAtomTask<ManualReviewUserTaskElement> {

    override fun getParamElement(task: PipelineBuildTask): ManualReviewUserTaskElement {
        return JsonUtil.mapTo((task.taskParams), ManualReviewUserTaskElement::class.java)
    }

    override fun execute(
        task: PipelineBuildTask,
        param: ManualReviewUserTaskElement,
        runVariables: Map<String, String>
    ): AtomResponse {

        val buildId = task.buildId
        val taskId = task.taskId
        val projectCode = task.projectId
        val pipelineId = task.pipelineId

        val reviewUsers = parseVariable(param.reviewUsers.joinToString(","), runVariables)
        val reviewDesc = parseVariable(param.desc, runVariables)

        if (reviewUsers.isBlank()) {
            logger.warn("[$buildId]|taskId=$taskId|Review user is empty")
            return AtomResponse(BuildStatus.FAILED)
        }

        // 开始进入人工审核步骤，需要打印日志，并发送通知给审核人
        buildLogPrinter.addYellowLine(
            buildId = task.buildId, message = "============步骤等待审核============",
            tag = taskId, jobId = task.containerHashId, executeCount = task.executeCount ?: 1
        )
        buildLogPrinter.addLine(
            buildId = task.buildId, message = "待审核人：$reviewUsers",
            tag = taskId, jobId = task.containerHashId, executeCount = task.executeCount ?: 1
        )
        buildLogPrinter.addLine(
            buildId = task.buildId, message = "审核说明：$reviewDesc",
            tag = taskId, jobId = task.containerHashId, executeCount = task.executeCount ?: 1
        )
        buildLogPrinter.addLine(
            buildId = buildId, message = "审核参数：${param.params.map { "{key=${it.key}, value=${it.value}}" }}",
            tag = taskId, jobId = task.containerHashId, executeCount = task.executeCount ?: 1
        )

        val pipelineName = runVariables[PIPELINE_NAME].toString()
        pipelineEventDispatcher.dispatch(
            PipelineBuildNotifyEvent(
                notifyTemplateEnum = PipelineNotifyTemplateEnum.PIPELINE_MANUAL_REVIEW_ATOM_NOTIFY_TEMPLATE.name,
                source = "ManualReviewTaskAtom", projectId = projectCode, pipelineId = pipelineId,
                userId = task.starter, buildId = buildId,
                receivers = reviewUsers.split(","),
                notifyType = checkNotifyType(param.notifyType),
                titleParams = mutableMapOf(
                    "content" to (param.notifyTitle ?: "")
                ),
                bodyParams = mutableMapOf(
                    "buildNum" to (runVariables[PIPELINE_BUILD_NUM] ?: "1"),
                    "projectName" to "need to add in notifyListener",
                    "pipelineName" to pipelineName,
                    "dataTime" to DateTimeUtil.formatDate(Date(), "yyyy-MM-dd HH:mm:ss"),
                    "reviewDesc" to reviewDesc
                )
            )
        )

        return AtomResponse(BuildStatus.REVIEWING)
    }

    override fun tryFinish(
        task: PipelineBuildTask,
        param: ManualReviewUserTaskElement,
        runVariables: Map<String, String>,
        force: Boolean
    ): AtomResponse {

        val taskId = task.taskId
        val buildId = task.buildId
        val manualAction = task.getTaskParam(BS_MANUAL_ACTION)
        val taskParam = JsonUtil.toMutableMap(task.taskParams)
        logger.info("[$buildId]|TRY_FINISH|${task.taskName}|taskId=$taskId|action=$manualAction")
        if (manualAction.isBlank()) {
            return AtomResponse(BuildStatus.REVIEWING)
        }

        val suggestContent = beforePrint(task = task, taskParam = taskParam)

        val response = when (ManualReviewAction.valueOf(manualAction)) {
            ManualReviewAction.PROCESS -> {
                buildLogPrinter.addLine(buildId = buildId, message = "审核结果：继续",
                    tag = taskId, jobId = task.containerHashId, executeCount = task.executeCount ?: 1
                )
                buildLogPrinter.addLine(buildId = buildId, message = "审核参数：${getParamList(taskParam)}",
                    tag = taskId, jobId = task.containerHashId, executeCount = task.executeCount ?: 1
                )
                AtomResponse(BuildStatus.SUCCEED)
            }
            ManualReviewAction.ABORT -> {
                buildLogPrinter.addRedLine(buildId = buildId, message = "审核结果：驳回",
                    tag = taskId, jobId = task.containerHashId, executeCount = task.executeCount ?: 1
                )
                AtomResponse(BuildStatus.REVIEW_ABORT)
            }
        }

        postPrint(param = param, task = task, suggestContent = suggestContent)
        return response
    }

    private fun getParamList(taskParam: MutableMap<String, Any>) =
        try {
            JsonUtil.getObjectMapper().readValue(taskParam[BS_MANUAL_ACTION_PARAMS].toString(), List::class.java)
        } catch (ignored: Exception) {
            null
        }

    private fun postPrint(param: ManualReviewUserTaskElement, task: PipelineBuildTask, suggestContent: Any?) {
        val manualAction = task.getTaskParam(BS_MANUAL_ACTION)
        val manualActionUserId = task.getTaskParam(BS_MANUAL_ACTION_USERID)
        val reviewResultParamKey = if (param.namespace.isNullOrBlank()) {
            MANUAL_REVIEW_ATOM_RESULT
        } else {
            "${param.namespace}_$MANUAL_REVIEW_ATOM_RESULT"
        }
        val reviewerParamKey = if (param.namespace.isNullOrBlank()) {
            MANUAL_REVIEW_ATOM_REVIEWER
        } else {
            "${param.namespace}_$MANUAL_REVIEW_ATOM_REVIEWER"
        }
        val suggestParamKey = if (param.namespace.isNullOrBlank()) {
            MANUAL_REVIEW_ATOM_SUGGEST
        } else {
            "${param.namespace}_$MANUAL_REVIEW_ATOM_SUGGEST"
        }
        pipelineVariableService.setVariable(
            buildId = task.buildId, projectId = task.projectId, pipelineId = task.pipelineId,
            varName = reviewResultParamKey, varValue = manualAction
        )
        pipelineVariableService.setVariable(
            buildId = task.buildId, projectId = task.projectId, pipelineId = task.pipelineId,
            varName = reviewerParamKey, varValue = manualActionUserId
        )
        pipelineVariableService.setVariable(
            buildId = task.buildId, projectId = task.projectId, pipelineId = task.pipelineId,
            varName = suggestParamKey, varValue = suggestContent ?: ""
        )
        buildLogPrinter.addYellowLine(
            buildId = task.buildId, message = "output(except): $reviewResultParamKey=$manualAction",
            tag = task.taskId, jobId = task.containerHashId, executeCount = task.executeCount ?: 1
        )
        buildLogPrinter.addYellowLine(
            buildId = task.buildId, message = "output(except): $reviewerParamKey=$manualActionUserId",
            tag = task.taskId, jobId = task.containerHashId, executeCount = task.executeCount ?: 1
        )
        buildLogPrinter.addYellowLine(
            buildId = task.buildId, message = "output(except): $suggestParamKey=$suggestContent",
            tag = task.taskId, jobId = task.containerHashId, executeCount = task.executeCount ?: 1
        )
    }

    private fun beforePrint(task: PipelineBuildTask, taskParam: MutableMap<String, Any>): Any? {
        val manualActionUserId = task.getTaskParam(BS_MANUAL_ACTION_USERID)
        val suggestContent = taskParam[BS_MANUAL_ACTION_SUGGEST]
        buildLogPrinter.addYellowLine(
            buildId = task.buildId, message = "============步骤审核结束============",
            tag = task.taskId, jobId = task.containerHashId, executeCount = task.executeCount ?: 1
        )
        buildLogPrinter.addLine(
            buildId = task.buildId, message = "审核人：$manualActionUserId",
            tag = task.taskId, jobId = task.containerHashId, executeCount = task.executeCount ?: 1
        )
        buildLogPrinter.addLine(
            buildId = task.buildId, message = "审核意见：$suggestContent",
            tag = task.taskId, jobId = task.containerHashId, executeCount = task.executeCount ?: 1
        )
        return suggestContent
    }

    private fun checkNotifyType(notifyType: MutableList<String>?): MutableSet<String>? {
        if (notifyType != null) {
            val allTypeSet = NotifyType.values().map { it.name }.toMutableSet()
            allTypeSet.remove(NotifyType.SMS.name)
            return (notifyType.toSet() intersect allTypeSet).toMutableSet()
        }
        return notifyType
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ManualReviewTaskAtom::class.java)
        const val MANUAL_REVIEW_ATOM_REVIEWER = "MANUAL_REVIEWER"
        const val MANUAL_REVIEW_ATOM_SUGGEST = "MANUAL_REVIEW_SUGGEST"
        const val MANUAL_REVIEW_ATOM_RESULT = "MANUAL_REVIEW_RESULT"
    }
}
