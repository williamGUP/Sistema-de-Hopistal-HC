/* Hospital HC — interacciones del subsistema de acceso.
   Todo es opcional: el sistema funciona igual con JavaScript desactivado
   (el menu de usuario usa <details> nativo y los formularios se envian
   con los metodos normales de HTML). */

(function () {
  "use strict";

  /* ---------- Mostrar / ocultar contrasena ---------- */

  function activarToggleContrasena() {
    document.querySelectorAll("[data-acc-toggle]").forEach(function (boton) {
      boton.addEventListener("click", function () {
        var input = document.getElementById(boton.getAttribute("data-acc-toggle"));
        if (!input) return;
        var visible = input.type === "text";
        input.type = visible ? "password" : "text";
        boton.textContent = visible ? "Ver" : "Ocultar";
        boton.setAttribute("aria-label", visible ? "Mostrar contrasena" : "Ocultar contrasena");
      });
    });
  }

  /* ---------- Medidor de fuerza de contrasena ---------- */

  function puntuar(password) {
    if (!password) return 0;
    var puntos = 0;
    if (password.length >= 8) puntos++;
    if (password.length >= 12) puntos++;
    if (/[a-z]/.test(password) && /[A-Z]/.test(password)) puntos++;
    if (/\d/.test(password) && /[^A-Za-z0-9]/.test(password)) puntos++;
    if (password.length < 8) return 1;
    return Math.min(4, Math.max(1, puntos));
  }

  var ETIQUETAS = ["", "Debil", "Aceptable", "Buena", "Excelente"];

  function activarMedidor() {
    var input = document.querySelector("[data-acc-medidor]");
    if (!input) return;
    var caja = document.querySelector("[data-acc-medidor-caja]");
    var texto = document.querySelector("[data-acc-medidor-texto]");
    if (!caja || !texto) return;

    function actualizar() {
      var nivel = puntuar(input.value);
      if (!input.value) {
        caja.removeAttribute("data-nivel");
        texto.textContent = "Minimo 8 caracteres, con letras y numeros.";
        return;
      }
      caja.setAttribute("data-nivel", String(nivel));
      texto.textContent = ETIQUETAS[nivel];
    }

    input.addEventListener("input", actualizar);
    actualizar();
  }

  /* ---------- Validacion de que la repeticion coincide ---------- */

  function activarConfirmacion() {
    var nueva = document.getElementById("passwordNueva");
    var repetir = document.getElementById("passwordRepetir");
    if (!nueva || !repetir) return;

    function validar() {
      if (!repetir.value) {
        repetir.setCustomValidity("");
        return;
      }
      var iguales = nueva.value === repetir.value;
      repetir.setCustomValidity(iguales ? "" : "Las contrasenas no coinciden");
      var aviso = document.querySelector("[data-acc-coincide]");
      if (aviso) aviso.hidden = iguales;
    }

    nueva.addEventListener("input", validar);
    repetir.addEventListener("input", validar);
  }

  /* ---------- Cuentas de demostracion: rellenan el formulario ---------- */

  function activarCuentasDemo() {
    var tarjetas = document.querySelectorAll("[data-acc-demo]");
    if (!tarjetas.length) return;

    var usuario = document.getElementById("username");
    var password = document.getElementById("password");

    tarjetas.forEach(function (tarjeta) {
      tarjeta.addEventListener("click", function () {
        if (!usuario || !password) return;
        var yaElegida = tarjeta.getAttribute("aria-pressed") === "true";
        tarjetas.forEach(function (otra) { otra.setAttribute("aria-pressed", "false"); });
        if (yaElegida) {
          usuario.value = "";
          password.value = "";
          password.focus();
          return;
        }
        tarjeta.setAttribute("aria-pressed", "true");
        usuario.value = tarjeta.getAttribute("data-acc-demo");
        password.value = tarjeta.getAttribute("data-acc-password");
        password.focus();
      });
    });
  }

  /* ---------- Estado de carga del boton de envio ---------- */

  function activarEnvio() {
    var formularios = document.querySelectorAll("[data-acc-form]");
    Array.prototype.forEach.call(formularios, function (formulario) {
      formulario.addEventListener("submit", function () {
        var boton = formulario.querySelector("[data-acc-enviar]");
        if (!boton || boton.disabled) return;
        boton.classList.add("is-loading");
        boton.disabled = true;
        boton.setAttribute("aria-busy", "true");
      });
    });
  }

  /* ---------- Menu de usuario: cerrar al pulsar fuera o con Escape ---------- */

  function activarMenu() {
    var menu = document.querySelector(".acc-menu");
    if (!menu) return;

    document.addEventListener("click", function (evento) {
      if (menu.open && !menu.contains(evento.target)) menu.open = false;
    });

    document.addEventListener("keydown", function (evento) {
      if (evento.key === "Escape" && menu.open) {
        menu.open = false;
        menu.querySelector("summary").focus();
      }
    });
  }

  /* ---------- Avisosflash que se ocultan solos ---------- */

  function activarAvisos() {
    var avisos = document.querySelectorAll("[data-acc-autohide]");
    Array.prototype.forEach.call(avisos, function (aviso) {
      window.setTimeout(function () {
        aviso.style.transition = "opacity .3s ease";
        aviso.style.opacity = "0";
        window.setTimeout(function () { aviso.hidden = true; }, 320);
      }, 7000);
    });
  }

  document.addEventListener("DOMContentLoaded", function () {
    activarToggleContrasena();
    activarMedidor();
    activarConfirmacion();
    activarCuentasDemo();
    activarEnvio();
    activarMenu();
    activarAvisos();
  });
})();
