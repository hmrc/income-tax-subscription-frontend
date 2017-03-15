$(document).ready(function () {
    var $day = $('input[name$="dateDay"]'),
        $month = $('input[name$="dateMonth"]'),
        $year = $('input[name$="dateYear"]');
    var BACKSPACE = 8;
    var ZERO = 48;
    var ZERO_NUMPAD = 96;
    var NINE = 57;
    var NINE_NUMPAD = 105;

    function dayKeyUp(key, elem) {
        var name = elem.name;
        var newValue = $(elem).val();
        var month = name.replace("dateDay", "dateMonth");

        if (key != BACKSPACE && newValue.length == 2) {
            $(document.getElementById(month)).focus();
        }
    }

    function monthKeyDown(key, elem) {
        var name = elem.name;
        var newValue = $(elem).val();
        var day = name.replace("dateMonth", "dateDay");
        var year = name.replace("dateMonth", "dateYear");

        if (key == BACKSPACE && newValue.length == 0) {
            $(document.getElementById(day)).focus();
        }
    }

    function monthKeyUp(key, elem) {
        var name = elem.name;
        var newValue = $(elem).val();
        var day = name.replace("dateMonth", "dateDay");
        var year = name.replace("dateMonth", "dateYear");

        if (key != BACKSPACE && newValue.length == 2) {
            $(document.getElementById(year)).focus();
        }
    }

    function yearKeyDown(key, elem) {
        var name = elem.name;
        var newValue = $(elem).val();
        var month = name.replace("dateYear", "dateMonth");

        if (key == BACKSPACE && newValue.length == 0) {
            $(document.getElementById(month)).focus();
        }
    }

    function inputFilter(event) {
        var key = event.which;
        if ((!modifier && key >= ZERO && key <= NINE) ||
            (!modifier && key >= ZERO_NUMPAD && key <= NINE_NUMPAD) ||
            key == BACKSPACE) {
            return true
        }
        event.preventDefault();
        return false
    }

    $day.on('keydown', function (e) {
        inputFilter(e);
    });

    $day.on('keyup', function (e) {
        var key = e.which;
        dayKeyUp(key, this);
    });

    $month.on('keydown', function (e) {
        if (inputFilter(e)) {
            var key = e.which;
            monthKeyDown(key, this);
        }
    });

    $month.on('keyup', function (e) {
        var key = e.which;
        monthKeyUp(key, this);
    });

    $year.on('keydown', function (e) {
        if (inputFilter(e)) {
            var key = e.which;
            yearKeyDown(key, this);
        }
    });

    var modifier = false;
    window.onkeydown = function (e) {
        if (e.which >= 16 && e.which <= 19) {
            modifier = true;
        }
    };
    window.onkeyup = function (e) {
        if (e.which >= 16 && e.which <= 19) {
            modifier = false;
        }
    };
});
