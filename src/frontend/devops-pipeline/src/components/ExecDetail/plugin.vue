<template>
    <detail-container @close="$emit('close')"
        :title="currentElement.name"
        :status="currentElement.status"
        :current-tab="currentTab"
        :is-hook="((currentElement.additionalOptions || {}).elementPostInfo || false)"
    >
        <span class="head-tab" slot="tab" v-if="showTab">
            <template v-for="tab in tabList">
                <span v-if="tab.show"
                    :key="tab.name"
                    :class="{ active: currentTab === tab.name }"
                    @click="currentTab = tab.name"
                >{{ $t(`execDetail.${tab.name}`) }}</span>
            </template>
        </span>
        <reference-variable slot="tool" class="head-tool" :global-envs="globalEnvs" :stages="stages" :container="container" v-if="currentTab === 'setting'" />
        <template v-slot:content>
            <plugin-log :id="currentElement.id"
                :build-id="execDetail.id"
                :current-tab="currentTab"
                :execute-count="currentElement.executeCount"
                ref="log"
                v-show="currentTab === 'log'"
            />
            <component :is="value.component"
                v-bind="value.bindData"
                v-for="(value, key) in componentList"
                :key="key"
                :ref="key"
                @hidden="hideTab(key)"
                @complete="completeLoading(key)"
                v-show="currentTab === key"
            ></component>
        </template>
    </detail-container>
</template>

<script>
    import { mapState } from 'vuex'
    import detailContainer from './detailContainer'
    import AtomContent from '@/components/AtomPropertyPanel/AtomContent.vue'
    import ReferenceVariable from '@/components/AtomPropertyPanel/ReferenceVariable'
    import pluginLog from './log/pluginLog'
    import Report from './Report'
    import Artifactory from './Artifactory'

    export default {
        components: {
            detailContainer,
            ReferenceVariable,
            pluginLog
        },

        data () {
            return {
                currentTab: 'log',
                tabList: [
                    { name: 'log', show: true },
                    { name: 'artifactory', show: true, completeLoading: false },
                    { name: 'report', show: true, completeLoading: false },
                    { name: 'setting', show: true }
                ]
            }
        },

        computed: {
            ...mapState('atom', [
                'execDetail',
                'editingElementPos',
                'globalEnvs'
            ]),

            stages () {
                return this.execDetail.model.stages
            },

            container () {
                const {
                    editingElementPos: { stageIndex, containerIndex },
                    execDetail: { model: { stages } }
                } = this
                return stages[stageIndex].containers[containerIndex]
            },

            currentElement () {
                const {
                    editingElementPos: { stageIndex, containerIndex, elementIndex },
                    execDetail: { model: { stages } }
                } = this
                return stages[stageIndex].containers[containerIndex].elements[elementIndex]
            },

            componentList () {
                return {
                    artifactory: {
                        component: Artifactory,
                        bindData: {
                            taskId: this.currentElement.id
                        }
                    },
                    report: {
                        component: Report,
                        bindData: {
                            taskId: this.currentElement.id
                        }
                    },
                    setting: {
                        component: AtomContent,
                        bindData: {
                            elementIndex: this.editingElementPos.elementIndex,
                            containerIndex: this.editingElementPos.containerIndex,
                            stageIndex: this.editingElementPos.stageIndex,
                            stages: this.stages,
                            editable: false,
                            isInstanceTemplate: false
                        }
                    }
                }
            },

            showTab () {
                return this.tabList[1].completeLoading && this.tabList[2].completeLoading
            }
        },

        methods: {
            hideTab (key) {
                const tab = this.tabList.find(tab => tab.name === key)
                tab.show = false
            },

            completeLoading (key) {
                const tab = this.tabList.find(tab => tab.name === key)
                tab.completeLoading = true
            }
        }
    }
</script>

<style lang="scss" scoped>
    ::v-deep .atom-property-panel {
        padding: 10px 50px;
        .bk-form-item.is-required .bk-label, .bk-form-inline-item.is-required .bk-label {
            margin-right: 10px;
        }
    }
    ::v-deep .reference-var {
        padding: 0;
    }
</style>
