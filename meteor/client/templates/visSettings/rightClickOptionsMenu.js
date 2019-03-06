Template.rightClickOptionsMenu.helpers({
    getRightClickTargetType() {
        return Session.get('rightClickTarget');
    },
    updateRightClickContent() {
        const selectedType = Session.get('rightClickTarget');
        console.log(selectedType)
        if (selectedType) {
            const atomColor = getAtomColor(selectedType);
            console.log(atomColor)
            $('.right-click-color-picker').prop('disabled', false);
            $('.right-click-shape-picker').val(getAtomShape(selectedType));
        }
    },
});

Template.rightClickOptionsMenu.events({
    'change .right-click-color-picker'(event) {
        const selectedType = Session.get('rightClickTarget');
        cy.nodes(`[type='${selectedType}']`).data({ color: event.target.value });
        updateAtomColor(selectedType, event.target.value);
        refreshGraph();
    },
    'change .right-click-shape-picker'(event) {
        const selectedType = Session.get('rightClickTarget');
        cy.nodes(`[type='${selectedType}']`).data({ shape: event.target.value });
        updateAtomShape(selectedType, event.target.value);
        refreshGraph();
    },
    'click #rightClickProject'() {
        const selectedType = Session.get('rightClickTarget');
        try {
            if (currentlyProjectedTypes.indexOf(selectedType) == -1)addTypeToProjection(selectedType);
            else removeTypeFromProjection(selectedType);
            $('#optionsMenu').hide();
            // TODO simular click na parte branca para limpar o checkBox

            // eventFire(document.getElementById(''), 'click');
        } catch (err) {
            console.log(err);
        }
    },
});

Template.rightClickOptionsMenu.onRendered(() => {
    $('#optionsMenu').hide();
});
