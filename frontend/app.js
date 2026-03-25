const healthEl = document.getElementById("health");
const checkBtn = document.getElementById("checkBtn");

async function checkBackend() {
  healthEl.textContent = "Checking API root endpoint...";
  try {
    const res = await fetch("http://localhost:8080/", { method: "GET" });
    if (!res.ok) {
      healthEl.textContent = `API responded with status ${res.status}.`;
      return;
    }
    const body = await res.json();
    healthEl.textContent = `API reachable. Message: ${body.message ?? "OK"}`;
  } catch (err) {
    healthEl.textContent = "API not reachable. Start backend then retry.";
  }
}

checkBtn.addEventListener("click", checkBackend);
checkBackend();
