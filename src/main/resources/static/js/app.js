const api = {
  async createExpense(payload) {
    const response = await fetch("/api/v1/expenses", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(payload)
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.message || "Failed to create expense");
    }

    return data;
  },

  async settleBalance(payload) {
    const response = await fetch("/api/v1/settlements", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(payload)
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.message || "Failed to settle balance");
    }

    return data;
  }
};

const dom = {
  expenseForm: document.getElementById("expenseForm"),
  settlementForm: document.getElementById("settlementForm"),
  expenseResult: document.getElementById("expenseResult"),
  settlementResult: document.getElementById("settlementResult"),
  globalError: document.getElementById("globalError"),
  loading: document.getElementById("loading")
};

function setLoading(isLoading) {
  if (!dom.loading) return;
  dom.loading.style.display = isLoading ? "block" : "none";
}

function showError(message) {
  if (!dom.globalError) return;
  dom.globalError.textContent = message;
  dom.globalError.style.display = "block";
}

function clearError() {
  if (!dom.globalError) return;
  dom.globalError.textContent = "";
  dom.globalError.style.display = "none";
}

function renderExpenseSuccess(data) {
  if (!dom.expenseResult) return;
  dom.expenseResult.innerHTML = `
    <div class="success-box">
      <h3>Expense Created</h3>
      <p><strong>Expense ID:</strong> ${data.expenseId}</p>
      <p><strong>Status:</strong> ${data.status}</p>
      <p><strong>Message:</strong> ${data.message}</p>
    </div>
  `;
}

function renderSettlementSuccess(data) {
  if (!dom.settlementResult) return;
  dom.settlementResult.innerHTML = `
    <div class="success-box">
      <h3>Settlement Recorded</h3>
      <p><strong>Settlement ID:</strong> ${data.settlementId}</p>
      <p><strong>Status:</strong> ${data.status}</p>
      <p><strong>Message:</strong> ${data.message}</p>
    </div>
  `;
}

function parseParticipants(rawParticipants, splitType) {
  return rawParticipants
    .split("\n")
    .map(line => line.trim())
    .filter(Boolean)
    .map(line => {
      const parts = line.split(",").map(v => v.trim());

      if (splitType === "EQUAL") {
        return { userId: parts[0] };
      }

      if (splitType === "EXACT") {
        return {
          userId: parts[0],
          exactAmount: Number(parts[1])
        };
      }

      if (splitType === "PERCENTAGE") {
        return {
          userId: parts[0],
          percentage: Number(parts[1])
        };
      }

      throw new Error("Unsupported split type: " + splitType);
    });
}

async function handleExpenseSubmit(event) {
  event.preventDefault();
  clearError();
  setLoading(true);

  try {
    const payload = {
      groupId: document.getElementById("expenseGroupId").value || null,
      paidByUserId: document.getElementById("paidByUserId").value,
      title: document.getElementById("expenseTitle").value,
      description: document.getElementById("expenseDescription").value,
      totalAmount: Number(document.getElementById("totalAmount").value),
      currencyCode: document.getElementById("currencyCode").value.toUpperCase(),
      splitType: document.getElementById("splitType").value,
      expenseDate: document.getElementById("expenseDate").value,
      createdByUserId: document.getElementById("createdByUserId").value,
      participants: parseParticipants(
        document.getElementById("participants").value,
        document.getElementById("splitType").value
      )
    };

    const result = await api.createExpense(payload);
    renderExpenseSuccess(result);
    dom.expenseForm.reset();
  } catch (error) {
    showError(error.message);
  } finally {
    setLoading(false);
  }
}

async function handleSettlementSubmit(event) {
  event.preventDefault();
  clearError();
  setLoading(true);

  try {
    const payload = {
      groupId: document.getElementById("settlementGroupId").value || null,
      paidByUserId: document.getElementById("settlementPaidByUserId").value,
      paidToUserId: document.getElementById("settlementPaidToUserId").value,
      amount: Number(document.getElementById("settlementAmount").value),
      currencyCode: document.getElementById("settlementCurrencyCode").value.toUpperCase(),
      settlementDate: document.getElementById("settlementDate").value,
      note: document.getElementById("settlementNote").value,
      createdByUserId: document.getElementById("settlementCreatedByUserId").value
    };

    const result = await api.settleBalance(payload);
    renderSettlementSuccess(result);
    dom.settlementForm.reset();
  } catch (error) {
    showError(error.message);
  } finally {
    setLoading(false);
  }
}

if (dom.expenseForm) {
  dom.expenseForm.addEventListener("submit", handleExpenseSubmit);
}

if (dom.settlementForm) {
  dom.settlementForm.addEventListener("submit", handleSettlementSubmit);
}
