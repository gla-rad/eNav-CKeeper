/**
 * A helper function sets a value to null if it's empty/undefined.
 *
 * @param {*} obj   The object to be checked whether empty
 */
function nullIfEmpty(obj) {
    if (obj && obj != "null" && obj != "undefined") {
        return obj;
    }
    return null;
}

/**
 * A helper function to handle error UI operations.
 *
 * @param {String}      text    The error text to be displayed
 */
function showErrorDialog(text, action) {
    // Initialise the confirmation dialog
    $('#errorDialog .modal-body').html(text);

    // And show the dialog
    $('#errorDialog').modal('show');
}