$(document).ready(function () {
    const tipo = $('#dataTable').attr('data-tipo-registros');
    $('#dataTable').DataTable({
        columnDefs: [{
            targets: 'fecha-dmy',
            render: function (valor, operacion) {
                if ((operacion === 'sort' || operacion === 'type') && /^\d{2}\/\d{2}\/\d{4}$/.test(valor)) {
                    return valor.split('/').reverse().join('-');
                }
                return valor;
            }
        }, {
            targets: 'precio-numerico',
            render: function (valor, operacion) {
                if (operacion === 'sort' || operacion === 'type') {
                    return valor === 'Sin precio' ? Number.MAX_SAFE_INTEGER : Number(valor.replace(/\D/g, ''));
                }
                return valor;
            }
        }, {
            targets: 'stock-numerico',
            render: function (valor, operacion) {
                if (operacion === 'sort' || operacion === 'type') {
                    return valor === 'Sin control' ? Number.MAX_SAFE_INTEGER : Number(valor);
                }
                return valor;
            }
        }],
        language: {
            emptyTable: 'No hay ' + tipo + ' disponibles',
            info: 'Mostrando del _START_ al _END_ de _TOTAL_ ' + tipo,
            infoEmpty: 'Mostrando 0 ' + tipo,
            infoFiltered: '(de un total de _MAX_ ' + tipo + ')',
            lengthMenu: 'Mostrar _MENU_ ' + tipo,
            loadingRecords: 'Cargando...',
            processing: 'Procesando...',
            search: 'Buscar:',
            zeroRecords: 'No se encontraron ' + tipo,
            paginate: {
                first: 'Primero',
                last: 'Último',
                next: 'Siguiente',
                previous: 'Anterior'
            },
            aria: {
                sortAscending: ': ordenar de manera ascendente',
                sortDescending: ': ordenar de manera descendente'
            }
        }
    });
});
