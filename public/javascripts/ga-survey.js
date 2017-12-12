$(document).ready($(function () {

    var $form = $("form");
    var $submissionButton = $("form button[type=submit]");

    $submissionButton.on('click', function (e) {
        var satisfactionSelection = $('input[name=satisfaction]:checked').val();

        var exitOrigin = getQueryVariable('origin')

        if (typeof ga === "function" && satisfactionSelection != undefined) {
            e.preventDefault();
            ga('send', 'event', 'itsa-exit-survey', 'satisfaction', satisfactionSelection);
            ga('send', 'event', 'itsa-exit-survey', 'origin', exitOrigin, {
                hitCallback: function () {
                    $form.submit();
                }
            });
        }
    });

}));

function getQueryVariable(variable) {
    var query = window.location.search.substring(1);
    var vars = query.split("&");
    for (var i=0;i<vars.length;i++) {
       var pair = vars[i].split("=");
       if(pair[0] == variable){return decodeURIComponent(pair[1]);}
    }
    return(false);
}