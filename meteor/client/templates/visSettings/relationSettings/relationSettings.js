Template.relationSettings.helpers({
    getRelation() {
        return Session.get('selectedRelation')
    }
})

// updates the content of the relations pane in the settings sidebar, including the
// current state of each property
updateOptionContentRelations = function () {
    const selectedRelation = Session.get('selectedRelation')
    if (selectedRelation) {
        $('#relationColorSettings').val(getRelationColor(selectedRelation))
        $('#relationLabelSettings').val(getRelationLabel(selectedRelation))
        $('#showAsArcs').prop('checked', isShowAsArcsOn(selectedRelation))
        $('#showAsAttributes').prop('checked', isShowAsAttributesOn(selectedRelation))
        $('#relationEdgeStyleSettings').val(getRelationEdgeStyle(selectedRelation))
    }
}

Template.relationSettings.events({
    'change #relationLabelSettings'(event) {
        const selectedRelation = Session.get('selectedRelation')
        cy.edges(`[relation='${selectedRelation}']`).data({ label: event.target.value })
        updateRelationLabel(selectedRelation, event.target.value)
        refreshGraph()
        refreshAttributes()
    },

    'change #relationColorSettings'(event) {
        const selectedRelation = Session.get('selectedRelation')
        cy.edges(`[relation='${selectedRelation}']`).data({ color: event.target.value })
        updateRelationColor(selectedRelation, event.target.value)
    },
    'change #showAsArcs'(event) {
        const selectedRelation = Session.get('selectedRelation')
        $(event.target).is(':checked') ? setShowAsArcsValue(selectedRelation, true) : setShowAsArcsValue(selectedRelation, false)
        updateShowAsArcs(selectedRelation, $(event.target).is(':checked'))
    },
    'change #showAsAttributes'(event) {
        const selectedRelation = Session.get('selectedRelation')
        $(event.target).is(':checked') ? setShowAsAttributesValue(selectedRelation, true) : setShowAsAttributesValue(selectedRelation, false)
        updateShowAsAttributes(selectedRelation, $(event.target).is(':checked'))
        refreshGraph()
    },
    'change #relationEdgeStyleSettings'(event) {
        const selectedRelation = Session.get('selectedRelation')
        cy.edges(`[relation='${selectedRelation}']`).data({ edgeStyle: event.target.value })
        updateEdgeStyle(selectedRelation, event.target.value)
    }
})

Template.relationSettings.onRendered(() => {
    $('.relation-settings').hide()
})
