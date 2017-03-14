$(document).ready(function () {
    var $day = $('input[name$="dateDay"]'),
        $month = $('input[name$="dateMonth"]'),
        $year = $('input[name$="dateYear"]');
    var BACKSPACE = 8;
    var ZERO = 48;
    var NINE = 57;

    function checkDay(key, elem) {
        var name = elem.name;
        var newValue = $(elem).val();
        var month = name.replace("dateDay", "dateMonth");

        if (newValue.length == 2) {
            $(document.getElementById(month)).focus();
        }
    }

    function checkMonth(key, elem) {
        var name = elem.name;
        var newValue = $(elem).val();
        var day = name.replace("dateMonth", "dateDay");
        var year = name.replace("dateMonth", "dateYear");

        console.log(key)
        if (key == BACKSPACE && newValue.length == 0) {
            $(document.getElementById(day)).focus();
        }

        if (newValue.length == 2) {
            $(document.getElementById(year)).focus();
        }

    }

    function checkYear(key, elem) {
        var name = elem.name;
        var newValue = $(elem).val();
        var month = name.replace("dateYear", "dateMonth");

        if (key == BACKSPACE && newValue.length == 0) {
            $(document.getElementById(month)).focus();
        }

    }

    function trigger(key) {
        return (key >= ZERO && key <= NINE ) || key == BACKSPACE
    }

    $day.on('keyup', function (e) {
        var key = e.which;
        if (trigger(key)) {
            checkDay(key, this);
        }
    });

    $month.on('keyup', function (e) {
        var key = e.which;
        if (trigger(key)) {
            checkMonth(key, this);
        }
    });

    $year.on('keyup', function (e) {
        var key = e.which;
        if (trigger(key)) {
            checkYear(key, this);
        }
    });
});
