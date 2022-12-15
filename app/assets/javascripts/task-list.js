document.addEventListener("DOMContentLoaded", function(event) {

    let continueButton = document.getElementById('continue-button')

    if(continueButton) {
        continueButton.addEventListener("click", function() {
                continueButton.disabled = true;
                continueButton.form.submit();
        });
    }

});