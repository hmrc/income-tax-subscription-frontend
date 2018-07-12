$(document).ready($(function () {

    $('[data-metrics]').each(function () {
        var metricsString = $(this).attr('data-metrics');
        var metrics = metricsString.split(';');
        metrics.forEach(function (metric) {
            var parts = metric.split(':');
            if (parts.length == 3) {
                ga('send', 'event', parts[0], parts[1], parts[2]);
            } else {
                ga('send', 'event', parts[0], parts[1], parts[2], parts[3]);
            }
        })
    });

}));
