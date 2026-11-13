(function () {
    // Loading state on forms.
    document.querySelectorAll("form[data-loading-form]").forEach(function (form) {
        form.addEventListener("submit", function () {
            var l = form.querySelector(".loading");
            if (l) { l.hidden = false; }
            form.querySelectorAll("button[type=submit]").forEach(function (b) { b.disabled = true; });
        });
    });

    // Intake screen: synthetic samples, local draft save/restore.
    var select = document.querySelector("[data-samples-target]");
    var jsonEl = document.getElementById("sample-cases");
    if (select && jsonEl) {
        var samples = JSON.parse(jsonEl.textContent);
        samples.forEach(function (s, i) {
            var o = document.createElement("option");
            o.value = String(i);
            o.textContent = s.label + " — " + s.expected;
            select.appendChild(o);
        });
        select.addEventListener("change", function () {
            var s = samples[Number(select.value)];
            if (!s) { return; }
            set("maskedCustomerRef", s.maskedCustomerRef); set("homeRegion", s.homeRegion); set("transactionRef", s.transactionRef);
            set("amount", s.amount); set("currency", s.currency); set("occurredAt", s.occurredAt); set("status", s.status);
            set("transactionChannel", s.transactionChannel); set("maskedMerchant", s.maskedMerchant); set("issueType", s.issueType);
            set("customerStatement", s.customerStatement); set("cardPossession", s.cardPossession); set("intakeChannel", s.intakeChannel);
            var mr = document.getElementById("merchantRecognised"); if (mr) { mr.checked = !!s.merchantRecognised; }
            var sd = document.getElementById("syntheticDataConfirmed"); if (sd) { sd.checked = true; }
            var rr = document.getElementById("reviewRequired"); if (rr) { rr.checked = true; }
        });
        var KEY = "fraudcase-intake-draft";
        var FIELDS = ["maskedCustomerRef","homeRegion","transactionRef","amount","currency","occurredAt","status",
            "transactionChannel","maskedMerchant","issueType","customerStatement","cardPossession","intakeChannel"];
        document.getElementById("save-draft").addEventListener("click", function () {
            var d = {};
            FIELDS.forEach(function (id) { var el = document.getElementById(id); if (el) { d[id] = el.value; } });
            localStorage.setItem(KEY, JSON.stringify(d));
            this.textContent = "Draft saved";
        });
        document.getElementById("restore-draft").addEventListener("click", function () {
            var d = JSON.parse(localStorage.getItem(KEY) || "null");
            if (!d) { this.textContent = "No draft saved"; return; }
            FIELDS.forEach(function (id) { if (d[id]) { set(id, d[id]); } });
        });
        function set(id, v) { var el = document.getElementById(id); if (el) { el.value = v; } }
    }

    // Review screen: show route selector only when REROUTE is chosen.
    var radios = document.querySelectorAll("input[name=disposition]");
    var routeRow = document.getElementById("route-row");
    function toggleRoute() {
        var checked = document.querySelector("input[name=disposition]:checked");
        if (routeRow) { routeRow.style.display = checked && checked.value === "REROUTE" ? "block" : "none"; }
    }
    radios.forEach(function (r) { r.addEventListener("change", toggleRoute); });
    toggleRoute();
})();
