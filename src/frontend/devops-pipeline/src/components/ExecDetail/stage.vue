<template>
    <detail-container @close="$emit('close')"
        :title="stage.name"
        :status="stage.status"
        :current-tab="currentTab"
    >
        <template v-slot:content>
            <stage-content :stage="stage"
                :stage-index="editingElementPos.stageIndex"
                :editable="false"
            />
        </template>
    </detail-container>
</template>

<script>
    import { mapState } from 'vuex'
    import detailContainer from './detailContainer'
    import StageContent from '@/components/StagePropertyPanel/StageContent.vue'

    export default {
        components: {
            detailContainer,
            StageContent
        },

        data () {
            return {
                currentTab: 'stage'
            }
        },

        computed: {
            ...mapState('atom', [
                'execDetail',
                'editingElementPos'
            ]),

            stage () {
                const { editingElementPos, execDetail } = this
                if (editingElementPos) {
                    const model = execDetail.model || {}
                    const stages = model.stages || []
                    const stage = stages[editingElementPos.stageIndex]
                    return stage
                }
                return null
            }
        }
    }
</script>

<style lang="scss" scoped>
    ::v-deep .stage-property-panel {
        padding: 10px 50px;
    }
</style>
