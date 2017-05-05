var open = false;

function markAccordionOpen(label) {
    if (!open) {
        open = true;
        ga('send', 'event', 'itsa', 'opened accordion', label);
    }
}
