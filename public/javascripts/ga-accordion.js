var open = false;

function markAccordionOpen(label) {
    if (!open) {
        open = true;
        console.log("Logged accordion open: " + label);
        ga('send', 'event', 'itsa', 'opened accordion', label);
    }
}
