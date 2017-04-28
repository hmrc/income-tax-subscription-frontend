$(document).ready($(function () {

    var $form = $("form");
    var $submissionButton = $("form button[type=submit]");

    $submissionButton.on('click', function (e) {
        e.preventDefault();
        var satisfactionSelection = $('input[name=satisfaction]:checked').val();
        console.log(satisfactionSelection);
        if (satisfactionSelection != undefined) {
            ga('send', 'event', 'itsa-exit-survey', 'satisfaction', satisfactionSelection, {
                hitCallback: function () {
                    $form.submit();
                }
            });
        } else {
            $form.submit();
        }
    });

}));