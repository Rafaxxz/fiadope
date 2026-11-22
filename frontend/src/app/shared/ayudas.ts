import { PasoAyuda } from './ayuda-modal';

/** Manual de usuario en línea: pasos de «¿Cómo funciona?» por pantalla. */
export const AYUDAS: Record<string, { titulo: string; pasos: PasoAyuda[]; nota?: string }> = {
  login: {
    titulo: '¿Cómo funciona FiadoPe?',
    pasos: [
      { titulo: 'Apunta el fiado en segundos', texto: 'Busca a tu vecino por su nombre o DNI, elige el producto y registra la compra a fin de mes o en cuotas desde el mostrador.' },
      { titulo: 'Cálculo automático de saldo', texto: 'Olvídate de sumar a mano. El sistema calcula intereses, cuotas y mora con las tasas pactadas y no deja fiar por encima del límite de crédito.' },
      { titulo: 'Estado de cuenta claro', texto: 'En la fecha de corte cada vecino recibe su estado de cuenta y puede revisarlo desde su celular en «Mi libreta».' },
    ],
    nota: 'Acceso seguro: contraseña fuerte, captcha y un código que llega a tu correo. Tras 5 intentos fallidos la cuenta se bloquea 15 minutos.',
  },
  tiendas: {
    titulo: '¿Cómo gestionar las tiendas?',
    pasos: [
      { titulo: 'Registrar una tienda', texto: 'Ingresa el RUC (11 dígitos, empieza con 10 o 20), el nombre, el propietario, el giro, la ubicación y el límite global de fiado. Su administrador ingresa con el RUC.' },
      { titulo: 'Dar de baja', texto: 'Una tienda dada de baja no puede ingresar: sus sesiones se cierran al instante. Puedes reactivarla cuando corresponda.' },
      { titulo: 'Topes del BCRP', texto: 'Actualiza cada semestre (mayo y noviembre) la TEA máxima compensatoria y moratoria. Ningún cliente puede pactar tasas por encima.' },
    ],
  },
  panel: {
    titulo: 'Guía rápida del panel',
    pasos: [
      { titulo: '¿Cómo registrar un fiado al instante?', texto: 'Presiona NUEVA COMPRA FIADA, escoge al vecino y el producto: verás el cálculo antes de confirmar.' },
      { titulo: 'Cortes de cada mes', texto: 'Cada cliente tiene su día de corte y su día de pago. En «Corte mensual» generas y envías los estados de cuenta.' },
      { titulo: 'Mora y transparencia', texto: 'Si el vecino paga después de su fecha de pago se cobra interés compensatorio y moratorio por cada día de atraso, en un ítem aparte.' },
    ],
  },
  catalogo: {
    titulo: '¿Cómo funciona el catálogo?',
    pasos: [
      { titulo: 'Dos precios por producto', texto: 'El precio contado es el de venta inmediata; el precio de lista es el que se cobra al fiado y nunca puede ser menor.' },
      { titulo: 'Modalidades', texto: 'Marca si el producto se puede fiar a fin de mes, en cuotas o de ambas formas. Debe tener al menos una.' },
      { titulo: 'Dar de baja', texto: 'Los productos dados de baja dejan de aparecer en las compras nuevas, pero se conservan en el historial.' },
    ],
  },
  clientes: {
    titulo: '¿Cómo registrar a un vecino?',
    pasos: [
      { titulo: 'Datos y consentimiento', texto: 'Ingresa DNI, nombres, dirección y teléfono. Sin la aceptación de la Política de Privacidad (Ley N.° 29733) no se puede registrar.' },
      { titulo: 'Condiciones del crédito', texto: 'Pacta la tasa (nominal o efectiva), la tasa moratoria, el límite de crédito, el número máximo de cuotas y los días de corte y de pago.' },
      { titulo: 'Topes y cambios', texto: 'Las tasas no pueden superar el tope del BCRP. Un cambio de tasa solo afecta compras futuras y queda en la bitácora.' },
    ],
  },
  compra: {
    titulo: '¿Cómo se calcula la compra?',
    pasos: [
      { titulo: 'Fin de mes', texto: 'Se paga todo en la próxima fecha de pago. El interés corre desde el día de la compra: S = C × (1 + TED)^días. Si compras después del corte, pasa al mes siguiente.' },
      { titulo: 'En cuotas', texto: 'Los días hasta la fecha de pago son de gracia total: sus intereses se suman al capital (C\'). Luego se pagan cuotas mensuales iguales (método francés).' },
      { titulo: 'Límite de crédito', texto: 'Si el monto supera el crédito disponible, la compra se rechaza.' },
    ],
  },
  cronograma: {
    titulo: '¿Cómo leer el cronograma?',
    pasos: [
      { titulo: 'Capital ajustado (C\')', texto: 'Es el precio de la compra más los intereses de los días de gracia. Sobre este monto se calculan las cuotas.' },
      { titulo: 'Cuota fija', texto: 'Todas las cuotas son iguales. En cada una el interés baja y la amortización sube; la última se ajusta por redondeo.' },
      { titulo: 'Saldo final', texto: 'Después de la última cuota el saldo llega a S/ 0.00.' },
    ],
  },
  corte: {
    titulo: '¿Cómo funciona el corte del mes?',
    pasos: [
      { titulo: 'Fecha de corte', texto: 'Ese día se cierran las compras del período de cada cliente. Las compras posteriores pasan al corte del mes siguiente.' },
      { titulo: 'Saldo al corte', texto: 'Suma las compras a fin de mes con su interés y las cuotas que vencen en la fecha de pago.' },
      { titulo: 'Generar y enviar', texto: 'Selecciona a los clientes y presiona «Generar y enviar estados de cuenta»: cada uno recibe el suyo por correo.' },
    ],
  },
  pago: {
    titulo: '¿Cómo se registra un pago?',
    pasos: [
      { titulo: 'Solo pago total', texto: 'El vecino cancela todo el estado de cuenta; no se aceptan pagos parciales.' },
      { titulo: 'Intereses por mora', texto: 'Si paga después de la fecha pactada se suma el interés compensatorio y el moratorio por cada día de atraso, en el ítem «Intereses por mora».' },
      { titulo: 'Libreta en cero', texto: 'Al confirmar, la libreta del período queda en S/ 0.00, se libera su crédito y el pago queda en la bitácora.' },
    ],
  },
  micuenta: {
    titulo: '¿Cómo leer mi libreta?',
    pasos: [
      { titulo: 'Total a pagar', texto: 'Es lo que debes hasta tu fecha límite. Si pasa la fecha, se suma un interés por cada día de atraso.' },
      { titulo: 'Crédito disponible', texto: 'Es lo que todavía puedes fiar en la tienda: tu línea de crédito menos tu deuda.' },
      { titulo: 'Compras y cuotas', texto: 'Revisa cada compra, la cuota que te toca y descarga tu estado de cuenta o tu cronograma en PDF.' },
    ],
  },
  bitacora: {
    titulo: '¿Para qué sirve la bitácora?',
    pasos: [
      { titulo: 'Trazabilidad', texto: 'Cada compra, pago, cambio de datos y acceso queda registrado con fecha, hora, usuario, IP y los valores anterior y nuevo.' },
      { titulo: 'Filtros', texto: 'Busca por usuario, tipo de acción, rango de fechas o dirección IP.' },
      { titulo: 'Inalterable', texto: 'Los registros no se pueden modificar ni borrar: sirven como evidencia ante cualquier reclamo.' },
    ],
  },
};

