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
}, {
    data: "entityType",
    title: "Entity Type",
    hoverMsg: "The type of the entity",
    placeholder: "The type of the entity",
    type: "select",
    options: {
        "device": "Device",
        "service": "Service",
        "vessel": "Vessel"
    },
    required: true
}, {
    data: "version",
    title: "Version (For Services)",
    hoverMsg: "The version of the entity",
    placeholder: "The version of the entity",
    required: false
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
    render: (data, type, row) => {
        return (data ? '<i class="fa-solid fa-circle-check" style="color:red"></i>' : '<i class="fa-solid fa-circle-xmark" style="color:green"></i>');
    },
    className: "dt-body-center",
    hoverMsg: "Revoked",
    placeholder: "Revoked",
    width: "10%"
}];

// Run when the document is ready
$(() => {
    mrnEntitiesTable = $('#mrn_entities_table').DataTable({
        "processing": true,
        "serverSide": true,
        ajax: {
            "type": "POST",
            "url": "./api/mrn-entity/dt",
            "contentType": "application/json",
            "data": (d) => {
                return JSON.stringify(d);
            },
            error: (response, status, more) => {
                error({"responseText" : response.getResponseHeader("X-cKeeper-error")}, status, more);
            }
        },
        columns: mrnEntitiesColumnDefs,
        dom: "<'row'<'col-lg-2 col-md-4'B><'col-lg-2 col-md-4'l><'col-lg-8 col-md-4'f>><'row'<'col-md-12'rt>><'row'<'col-md-6'i><'col-md-6'p>>",
        select: 'single',
        lengthMenu: [10, 25, 50, 75, 100],
        responsive: true,
        altEditor: true, // Enable altEditor
        buttons: [{
            text: '<i class="fa-solid fa-plus"></i>',
            titleAttr: 'Add Entity',
            name: 'add' // do not change name
        }, {
            extend: 'selected', // Bind to Selected row
            text: '<i class="fa-solid fa-pen-to-square"></i>',
            titleAttr: 'Edit Entity',
            name: 'edit' // do not change name
        }, {
            extend: 'selected', // Bind to Selected row
            text: '<i class="fa-solid fa-trash"></i>',
            titleAttr: 'Delete Entity',
            name: 'delete' // do not change name
        }, {
            extend: 'selected', // Bind to Selected row
            text: '<i class="fa-solid fa-certificate"></i>',
            titleAttr: 'View Entity Certificates',
            name: 'certificates', // do not change name
            className: 'certificates-toggle',
            action: (e, dt, node, config) => {
                loadMrnEntityCertificates(e, dt, node, config)
            }
        }],
        onAddRow: (datatable, rowdata, success, error) => {
            $.ajax({
                url: './api/mrn-entity',
                type: 'POST',
                contentType: 'application/json; charset=utf-8',
                dataType: 'json',
                data: JSON.stringify({
                    id: null,
                    name: nullIfEmpty(rowdata["name"]),
                    mrn: nullIfEmpty(rowdata["mrn"]),
                    mmsi: nullIfEmpty(rowdata["mmsi"]),
                    entityType: nullIfEmpty(rowdata["entityType"]),
                    version: nullIfEmpty(rowdata["version"])
                }),
                success: success,
                error: (response, status, more) => {
                    error({"responseText" : response.getResponseHeader("X-cKeeper-error")}, status, more);
                }
            });
        },
        onDeleteRow: (datatable, selectedRows, success, error) => {
             selectedRows.every((rowIdx, tableLoop, rowLoop) => {
                 $.ajax({
                     url: `./api/mrn-entity/${this.data()["id"]}`,
                     type: 'DELETE',
                     success: success,
                     error: (response, status, more) => {
                         error({"responseText" : response.getResponseHeader("X-cKeeper-error")}, status, more);
                     }
                 });
             });
        },
        onEditRow: (datatable, rowdata, success, error) => {
            $.ajax({
                type: 'PUT',
                url: `./api/mrn-entity/${rowdata["id"]}`,
                crossDomain: true,
                contentType: 'application/json; charset=utf-8',
                dataType: 'json',
                data: JSON.stringify({
                    id: nullIfEmpty(rowdata["id"]),
                    name: nullIfEmpty(rowdata["name"]),
                    mrn: nullIfEmpty(rowdata["mrn"]),
                    mmsi: nullIfEmpty(rowdata["mmsi"]),
                    entityType: nullIfEmpty(rowdata["entityType"]),
                    version: nullIfEmpty(rowdata["version"])
                }),
                success: success,
                error: (response, status, more) => {
                    error({"responseText" : response.getResponseHeader("X-cKeeper-error")}, status, more);
                }
            });
        }
    });

    // We also need to link the certificates toggle button with the the modal
    // panel so that by clicking the button the panel pops up. It's easier done
    // with jQuery.
    mrnEntitiesTable.buttons('.certificates-toggle')
        .nodes()
        .attr({ "data-bs-toggle": "modal", "data-bs-target": "#certificatesPanel" });

    // On confirmation of the certificate generation, we need to make an AJAX
    // call back to the service to generate a new certificate
    $('#confirm-generate-certificate').on('click', '.btn-ok', (e) => {
        var $modalDiv = $(e.delegateTarget);
        var idx = mrnEntitiesTable.cell('.selected', 0).index();
        var data = mrnEntitiesTable.rows(idx.row).data();
        var mrnEntityId = data[0].id;
        $modalDiv.addClass('loading');
        $.ajax({
            url: `./api/mrn-entity/${mrnEntityId}/certificates`,
            type: 'PUT',
            contentType: 'application/json; charset=utf-8',
            dataType: 'json',
            success: () => {
                certificatesTable.ajax.reload();
                $modalDiv.modal('hide').removeClass('loading');
            },
            error: (response, status, more)  => {
                $modalDiv.removeClass('loading');
                showErrorDialog(response.getResponseHeader("X-cKeeper-error"));
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
            url: `./api/certificate/${certificateId}/revoke`,
            type: 'PUT',
            contentType: 'application/json; charset=utf-8',
            dataType: 'json',
            success: () => {
                certificatesTable.ajax.reload();
                $modalDiv.modal('hide').removeClass('loading');
            },
            error: (response, status, more) => {
                $modalDiv.removeClass('loading');
                showErrorDialog(response.getResponseHeader("X-cKeeper-error"));
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
    var mrnEntityType = data[0].entityType;

    // Destroy the table if it already exists
    if (certificatesTable) {
        certificatesTable.destroy();
        certificatesTable = undefined;
    }

    // And re-initialise it
    certificatesTable = $('#certificates_table').DataTable({
        ajax: {
            "type": "GET",
            "url": `./api/mrn-entity/${mrnEntityId}/certificates`,
            "dataType": "json",
            "cache": false,
            "dataSrc": (json) => {
                // Place the content inside a textarea to escape the XML
                json.forEach(node => {
                    node["publicKey"] = "<textarea style=\"width: 100%; max-height: 300px\" readonly>"
                     + node["publicKey"]
                     + "</textarea>";
                });
                return json;
            },
            error: (response, status, more) => {
                showErrorDialog(response.getResponseHeader("X-cKeeper-error"));
            }
        },
        columns: certificatesColumnDefs,
        dom: "<'row'<'col-lg-2 col-md-4'B><'col-lg-2 col-md-4'l><'col-lg-8 col-md-4'f>><'row'<'col-md-12'rt>><'row'<'col-md-6'i><'col-md-6'p>>",
        select: 'single',
        autoWidth: false,
        lengthMenu: [10, 25, 50, 75, 100],
        responsive: true,
        altEditor: true, // Enable altEditor
        buttons: [{
            text: '<i class="fa-solid fa-gears"></i>',
            titleAttr: 'Generate New Certificate',
            name: 'generateCertificate', // do not change name
            action: (e, dt, node, config) => {
                $('#confirm-generate-certificate').modal('show');
            }
        }, {
            extend: 'selected', // Bind to Selected row
            text: '<i class="fa-solid fa-trash"></i>',
            titleAttr: 'Delete Certificate',
            name: 'delete' // do not change name
        }, {
            extend: 'selected', // Bind to Selected row
            text: '<i class="fa-solid fa-square-minus"></i>',
            titleAttr: 'Revoke Certificate',
            name: 'revokeCertificate', // do not change name
            action: (e, dt, node, config) => {
                $('#confirm-revoke-certificate').modal('show');
            }
        }],
        onDeleteRow: (datatable, selectedRows, success, error) => {
            selectedRows.every((rowIdx, tableLoop, rowLoop) => {
                if(!this.data()["revoked"] || this.data()["revoked"]=="false") {
                    $(datatable.modal_selector).modal('hide');
                    $('.reveal-overlay').hide();
                    showErrorDialog("You can only delete a certificate if it has first been revoked.\nPlease revoke it and try again...");
                } else {
                    $.ajax({
                        url: `./api/certificate/${this.data()["id"]}`,
                        type: 'DELETE',
                        crossDomain: true,
                        success: success,
                        error: (response, status, more) => {
                            error({"responseText" : response.getResponseHeader("X-cKeeper-error")}, status, more);
                        }
                    });
                }
            }):
        }
    });
}
