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

package com.tencent.devops.common.pipeline.container

import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildNo
import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线模型-构建触发容器")
data class TriggerContainer(
    @ApiModelProperty("构建容器序号id", required = false, hidden = true)
    override var id: String? = null,
    @ApiModelProperty("容器名称", required = true)
    override var name: String = "",
    @ApiModelProperty("任务集合", required = true)
    override var elements: List<Element> = listOf(),
    @ApiModelProperty("状态", required = true, hidden = true)
    override var status: String? = null,
    override var startEpoch: Long? = null,
    @ApiModelProperty("系统运行时间", required = false, hidden = true)
    override var systemElapsed: Long? = null,
    @ApiModelProperty("插件运行时间", required = false, hidden = true)
    override var elementElapsed: Long? = null,
    @ApiModelProperty("参数化构建", required = false)
    var params: List<BuildFormProperty> = listOf(),
    @ApiModelProperty("模板参数构建", required = false)
    val templateParams: List<BuildFormProperty>? = null,
    @ApiModelProperty("构建版本号", required = false)
    val buildNo: BuildNo? = null,
    @ApiModelProperty("是否可重试-仅限于构建详情展示重试，目前未作为编排的选项，暂设置为null不存储", required = false, hidden = true)
    override var canRetry: Boolean? = null,
    override var containerId: String? = null,
    @ApiModelProperty("构建环境启动状态", required = false, hidden = true)
    override var startVMStatus: String? = null,
    @ApiModelProperty("容器运行次数", required = false, hidden = true)
    override var executeCount: Int? = 0,
    @ApiModelProperty("用户自定义ID", required = false, hidden = false)
    override val jobId: String? = null,
    @ApiModelProperty("是否包含post任务标识", required = false, hidden = true)
    override var containPostTaskFlag: Boolean? = null
) : Container {
    companion object {
        const val classType = "trigger"
    }

    override fun getClassType() = classType
}
