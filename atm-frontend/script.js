let currentPin = "";   // ✅ MUST exist at top of script.js

// Ensure PIN input is cleared on page load/refresh
window.addEventListener('load', () => {
    const pinEl = document.getElementById('pin');
    if (pinEl) {
        try {
            // aggressively clear value to defeat browser autofill on refresh
            pinEl.placeholder = 'Enter PIN';
            pinEl.setAttribute('autocomplete', 'new-password');
            // temporarily remove name to avoid some browser heuristics
            const oldName = pinEl.name || '';
            pinEl.name = '';
            // toggle type to avoid autofill restoring the value
            pinEl.type = 'text';
            pinEl.value = '';
            pinEl.type = 'password';
            // restore name after short delay
            setTimeout(() => { pinEl.name = oldName || 'pin'; }, 50);
        } catch (e) {}
    }
});

/* LOGIN */
function login() {
    const pin = document.getElementById("pin").value;

    if (pin.trim() === "") {
        alert("Please enter PIN");
        return;
    }

    console.log("Sending login request for PIN:", pin); // debug

    fetch(`http://localhost:8080/api/atm/login?pin=${pin}`, {
        method: "POST"
    })
    .then(res => {
        if (!res.ok) {
            throw new Error("User does not exist");
        }
        return res.json();
    })
    .then(data => {
        console.log("Login success:", data); // debug

        currentPin = pin;

        document.getElementById("user").innerText =
            "Welcome, " + data.username;

        document.getElementById("balance").innerText =
            "Balance: ₹" + data.balance;

        document.getElementById("actions").style.display = "block";
        document.getElementById("loginSection").style.display = "none";
    })
    .catch(err => {
        console.error(err);
        alert(err.message);
    });
}

/* DEPOSIT */
function deposit() {
    const amountInput = document.getElementById("amount").value;
    const amount = parseFloat(amountInput);

    // ✅ Validation
    if (amountInput === "" || isNaN(amount) || amount <= 0) {
        alert("⚠️ Please enter a valid deposit amount");
        return; // stop execution
    }

    fetch(`http://localhost:8080/api/atm/deposit?pin=${currentPin}&amount=${amount}`, {
        method: "POST"
    })
    .then(res => res.json())
    .then(data => {
        document.getElementById("balance").innerText =
            "Balance: ₹" + data.balance;
        alert("Deposit successful");
    })
    .catch(() => alert("Transaction failed"));
}

/* WITHDRAW */
function withdraw() {
    const amountInput = document.getElementById("amount").value;
    const amount = parseFloat(amountInput);

    // ✅ Validation
    if (amountInput === "" || isNaN(amount) || amount <= 0) {
        alert("⚠️ Please enter a valid withdrawal amount");
        return; // stop execution
    }

    // Show denomination selection modal, then POST selection to backend
    showDenominationModal(amount, function(selection) {
        const payload = {
            pin: currentPin,
            amount: amount,
            denominations: selection
        };

        fetch(`http://localhost:8080/api/atm/withdraw?pin=${currentPin}&amount=${amount}`, {
            method: "POST",
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        })
        .then(res => {
            if (!res.ok) throw new Error("Invalid amount / insufficient balance");
            return res.json();
        })
        .then(data => {
            document.getElementById("balance").innerText =
                "Balance: ₹" + data.balance;
            alert("Please collect cash");
        })
        .catch(err => alert(err.message));
    });
}

// Create a simple modal to pick counts for each denomination
function showDenominationModal(amount, onConfirm) {
    const denoms = [500,200,100,50,20,10];
    // remove existing modal
    const existing = document.getElementById('denomModal');
    if (existing) existing.remove();

    const overlay = document.createElement('div');
    overlay.id = 'denomModal';
    overlay.style = 'position:fixed;left:0;top:0;right:0;bottom:0;background:rgba(0,0,0,0.5);display:flex;align-items:center;justify-content:center;z-index:9999;';

    const box = document.createElement('div');
    box.style = 'background:white;padding:16px;border-radius:8px;min-width:300px;';

    const title = document.createElement('h3');
    title.innerText = 'Select denominations for ₹' + amount;
    box.appendChild(title);

    const form = document.createElement('div');
    form.style = 'display:flex;flex-direction:column;gap:8px;';

    const inputs = {};
    denoms.forEach(d => {
        const row = document.createElement('div');
        row.style = 'display:flex;justify-content:space-between;align-items:center;';
        const lbl = document.createElement('label');
        lbl.innerText = d + ' : ';
        const inp = document.createElement('input');
        inp.type = 'number';
        inp.min = 0;
        inp.value = 0;
        inp.style = 'width:80px;';
        row.appendChild(lbl);
        row.appendChild(inp);
        form.appendChild(row);
        inputs[d] = inp;
    });

    const info = document.createElement('div');
    info.style = 'margin-top:8px;';
    info.innerText = 'Total selected: 0';
    form.appendChild(info);

    // update total when inputs change
    Object.values(inputs).forEach(inp => {
        inp.addEventListener('input', () => {
            let total = 0;
            denoms.forEach(d => { total += d * (parseInt(inputs[d].value) || 0); });
            info.innerText = 'Total selected: ' + total;
        });
    });

    const btnRow = document.createElement('div');
    btnRow.style = 'display:flex;gap:8px;justify-content:flex-end;margin-top:12px;';
    const cancel = document.createElement('button');
    cancel.innerText = 'Cancel';
    cancel.onclick = () => overlay.remove();
    const confirm = document.createElement('button');
    confirm.innerText = 'Confirm';
    confirm.onclick = () => {
        let total = 0;
        const selection = {};
        denoms.forEach(d => {
            const cnt = parseInt(inputs[d].value) || 0;
            if (cnt > 0) selection[d] = cnt;
            total += d * cnt;
        });
        if (total !== amount) {
            alert('Selected total must equal the requested amount (₹' + amount + ')');
            return;
        }
        overlay.remove();
        onConfirm(selection);
    };
    btnRow.appendChild(cancel);
    btnRow.appendChild(confirm);
    form.appendChild(btnRow);

    box.appendChild(form);
    overlay.appendChild(box);
    document.body.appendChild(overlay);
}

/* CHECK BALANCE */
function getBalance() {
    fetch(`http://localhost:8080/api/atm/balance?pin=${currentPin}`)
        .then(res => {
            if (!res.ok) {
                throw new Error("Unable to fetch balance");
            }
            return res.json();
        })
        .then(balance => {
            document.getElementById("balance").innerText =
                "Balance: ₹" + balance;
        })
        .catch(err => alert(err.message));
}


/* MINI STATEMENT PDF */
function downloadStatement() {
    window.open(
        `http://localhost:8080/api/atm/statement/pdf?pin=${currentPin}`,
        "_blank"
    );
}

/* LOGOUT */
function logout() {
    currentPin = "";
    document.getElementById("user").innerText = "";
    document.getElementById("balance").innerText = "";
    document.getElementById("actions").style.display = "none";
    document.getElementById("loginSection").style.display = "block";
    document.getElementById("pin").value = "";
    document.getElementById("amount").value = "";
}
