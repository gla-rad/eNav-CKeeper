/**
 * Global variables
 */
var mrnEntitiesTable = undefined;
var certificatesTable = undefined;

/**
 * The MRN Entities Table Column Definitions
 * @type {Array}
 */
var mrnEntitiesColumnDefs = [{
    data: "id",
    title: "ID",
    type: "hidden",
    visible: false,
    searchable: false
}, {
    data: "name",
    title: "Name",
    hoverMsg: "Name of the entity",
    placeholder: "Name of the entity",
    required: true,
}, {
    data: "mrn",
    title: "MRN",
    hoverMsg: "MRN of the entity",
    placeholder: "MRN of the entity",
    required: true,
}, {
       data: "mmsi",
       title: "MMSI",
       hoverMsg: "MMSI of the entity",
       placeholder: "MMSI of the entity"
 }];

/**
 * The MRN Entities Table Column Definitions
 * @type {Array}
 */
var certificatesColumnDefs = [{
    data: "id",
    title: "ID",
    type: "hidden",
    visible: false,
    searchable: false
}, {
    data: "mrnEntityId",
    title: "MRN Entity ID",
    type: "hidden",
    visible: false,
    searchable: false
}, {
    data: "publicKey",
    title: "Public Key",
    hoverMsg: "The Certificate Public Key",
    placeholder: "The Certificate Public Key",
    required: true,
    width: "40%"
}, {
    data: "startDate",
    title: "Start Date",
    type: "date",
    dateFormat: "MMM DD, YYYY",
    render: (data) => {
        return data ? moment(data).format("MMM DD, YYYY") : null;
    },
    hoverMsg: "The Certificate is Valid From",
    placeholder: "The Certificate is Valid From",
    width: "25%"
}, {
    data: "endDate",
    title: "End Date",
    type: "date",
    dateFormat: "MMM DD, YYYY",
    render: (data) => {
        return data ? moment(data).format("MMM DD, YYYY") : null;
    },
    hoverMsg: "The Certificate is Valid Until",
    placeholder: "The Certificate is Valid Until",
    width: "25%"
}, {
    data: "revoked",
    title: "Revoked",
    type: "select",
    options: {
        true: "true",
        false: "false"
    },
    render: function (data, type, row) {
        return (data ? '<i class="fas fa-check" style="color:red"></i>' : '<i class="fas fa-times-circle" style="color:green"></i>');
    },
    className: "dt-body-center",
    hoverMsg: "Revoked",
    placeholder: "Revoked",
    width: "10%"
}];

// Run when the document is ready
$(document).ready( function () {
    mrnEntitiesTable = $('#mrn_entities_table').DataTable({
        //"processing": true,
        //"language": {
        //    processing: '<i class="fa fa-spinner fa-spin fa-3x fa-fw"></i><span class="sr-only">Loading...</span>',
        //},
        "serverSide": true,
        ajax: {
            "type": "POST",
            "url": "/api/mrn-entities/dt",
            "contentType": "application/json",
            "data": function (d) {
                return JSON.stringify(d);
            },
            error: function (jqXHR, ajaxOptions, thrownError) {
                console.error(thrownError);
            }
        },
        columns: mrnEntitiesColumnDefs,
        dom: 'Bfrltip',
        select: 'single',
        lengthMenu: [10, 25, 50, 75, 100],
        responsive: true,
        altEditor: true, // Enable altEditor
        buttons: [{
            text: '<i class="fas fa-plus-circle"></i>',
            titleAttr: 'Add Entity',
            name: 'add' // do not change name
        }, {
            extend: 'selected', // Bind to Selected row
            text: '<i class="fas fa-edit"></i>',
            titleAttr: 'Edit Entity',
            name: 'edit' // do not change name
        }, {
            extend: 'selected', // Bind to Selected row
            text: '<i class="fas fa-trash-alt"></i>',
            titleAttr: 'Delete Entity',
            name: 'delete' // do not change name
        }, {
            extend: 'selected', // Bind to Selected row
            text: '<i class="fas fa-certificate"></i>',
            titleAttr: 'View Entity Certificates',
            name: 'certificates', // do not change name
            className: 'certificates-toggle',
            action: (e, dt, node, config) => {
                loadMrnEntityCertificates(e, dt, node, config)
            }
        }],
        onAddRow: function (datatable, rowdata, success, error) {
            $.ajax({
                url: '/api/mrn-entities',
                type: 'POST',
                contentType: 'application/json; charset=utf-8',
                dataType: 'json',
                data: JSON.stringify({
                    id: rowdata["id"],
                    name: rowdata["name"],
                    mrn: rowdata["mrn"],
                    mmsi: rowdata["mmsi"]
                }),
                success: success,
                error: error
            });
        },
        onDeleteRow: function (datatable, rowdata, success, error) {
            $.ajax({
                url: `/api/mrn-entities/${rowdata["id"]}`,
                type: 'DELETE',
                success: success,
                error: error
            });
        },
        onEditRow: function (datatable, rowdata, success, error) {
            $.ajax({
                url: `/api/mrn-entities/${rowdata["id"]}`,
                type: 'PUT',
                contentType: 'application/json; charset=utf-8',
                dataType: 'json',
                data: JSON.stringify({
                    id: rowdata["id"],
                    name: rowdata["name"],
                    mrn: rowdata["mrn"],
                    mmsi: rowdata["mmsi"]
                }),
                success: success,
                error: error
            });
        }
    });

    // We also need to link the certificates toggle button with the the modal
    // panel so that by clicking the button the panel pops up. It's easier done
    // with jQuery.
    mrnEntitiesTable.buttons('.certificates-toggle')
        .nodes()
        .attr({ "data-toggle": "modal", "data-target": "#certificatesPanel" });

    // On confirmation of the certificate generation, we need to make an AJAX
    // call back to the service to generate a new certificate
    $('#confirm-generate-certificate').on('click', '.btn-ok', (e) => {
        var $modalDiv = $(e.delegateTarget);
        var idx = mrnEntitiesTable.cell('.selected', 0).index();
        var data = mrnEntitiesTable.rows(idx.row).data();
        var mrnEntityId = data[0].id;
        $modalDiv.addClass('loading');
        $.ajax({
            url: `/api/mrn-entities/${mrnEntityId}/certificates`,
            type: 'PUT',
            contentType: 'application/json; charset=utf-8',
            dataType: 'json',
            success: () => {
                certificatesTable.ajax.reload();
                $modalDiv.modal('hide').removeClass('loading');
            },
            error: (jqXHR, textStatus, errorThrown)  => {
                $modalDiv.removeClass('loading');
                showError(jqXHR.responseJSON.message);
            }
        });
    });

    // On confirmation of the certificate revoke, we need to make an AJAX
    // call back to the service to revoke the selected certificate
    $('#confirm-revoke-certificate').on('click', '.btn-ok', (e) => {
        var $modalDiv = $(e.delegateTarget);
        var idx = certificatesTable.cell('.selected', 0).index();
        var data = certificatesTable.rows(idx.row).data();
        var certificateId = data[0].id;
        $modalDiv.addClass('loading');
        $.ajax({
            url: `/api/certificates/${certificateId}/revoke`,
            type: 'PUT',
            contentType: 'application/json; charset=utf-8',
            dataType: 'json',
            success: () => {
                certificatesTable.ajax.reload();
                $modalDiv.modal('hide').removeClass('loading');
            },
            error: (jqXHR, textStatus, errorThrown)  => {
                $modalDiv.removeClass('loading');
                showError(jqXHR.responseJSON.message);
            }
        });
    });
});

/**
 * This function will initialise the certificated_table DOM element and loads
 * the certificates applicable for the provided row's MRN entity ID.
 *
 * @param {Event}         event         The event that took place
 * @param {DataTable}     table         The MRN entities table
 * @param {Node}          button        The button node that was pressed
 * @param {Configuration} config        The table configuration
 */
function loadMrnEntityCertificates(event, table, button, config) {
    var idx = table.cell('.selected', 0).index();
    var data = table.rows(idx.row).data();
    var mrnEntityId = data[0].id;

    // Destroy the table if it already exists
    if (certificatesTable) {
        certificatesTable.destroy();
        certificatesTable = undefined;
    }

    // And re-initialise it
    certificatesTable = $('#certificates_table').DataTable({
        ajax: {
            "type": "GET",
            "url": `/api/mrn-entities/${mrnEntityId}/certificates`,
            "dataType": "json",
            "cache": false,
            "dataSrc": function (json) {
                // Place the content inside a textarea to escape the XML
                json.forEach(node => {
                    node["publicKey"] = "<textarea style=\"width: 100%; max-height: 300px\" readonly>"
                     + node["publicKey"]
                     + "</textarea>";
                });
                return json;
            },
            error: function (jqXHR, ajaxOptions, thrownError) {
                console.error(thrownError);
            }
        },
        columns: certificatesColumnDefs,
        dom: 'Bfrltip',
        select: 'single',
        autoWidth: false,
        lengthMenu: [10, 25, 50, 75, 100],
        responsive: true,
        altEditor: true, // Enable altEditor
        buttons: [{
            text: '<i class="fas fa-cogs"></i>',
            titleAttr: 'Generate New Certificate',
            name: 'generateCertificate', // do not change name
            action: (e, dt, node, config) => {
                $('#confirm-generate-certificate').modal('show');
            }
        }, {
            extend: 'selected', // Bind to Selected row
            text: '<i class="fas fa-trash-alt"></i>',
            titleAttr: 'Delete Certificate',
            name: 'delete' // do not change name
        }, {
            extend: 'selected', // Bind to Selected row
            text: '<i class="fas fa-minus-square"></i>',
            titleAttr: 'Revoke Certificate',
            name: 'revokeCertificate', // do not change name
            action: (e, dt, node, config) => {
                $('#confirm-revoke-certificate').modal('show');
            }
        }],
        onDeleteRow: function (datatable, rowdata, success, error) {
            $.ajax({
                url: `/api/certificates/${rowdata["id"]}`,
                type: 'DELETE',
                success: success,
                error: error
            });
        }
    });
}
