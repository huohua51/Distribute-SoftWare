const result = document.getElementById("result");
const healthResult = document.getElementById("health-result");
const searchResult = document.getElementById("search-result");
const rwResult = document.getElementById("rw-result");

async function renderJson(target, promise) {
  target.textContent = "请求中...";
  try {
    const response = await promise;
    const data = await response.json();
    target.textContent = JSON.stringify(data, null, 2);
  } catch (error) {
    target.textContent = error.message;
  }
}

document.querySelectorAll("[data-product]").forEach((button) => {
  button.addEventListener("click", () => {
    const { product } = button.dataset;
    renderJson(result, fetch(`/api/products/${product}`));
  });
});

document.getElementById("health").addEventListener("click", () => {
  renderJson(healthResult, fetch("/api/health"));
});

document.getElementById("burst").addEventListener("click", async () => {
  result.textContent = "并发请求中...";
  try {
    const responses = await Promise.all(
      Array.from({ length: 20 }, () => fetch("/api/products/1").then((r) => r.json()))
    );

    const summary = responses.reduce((acc, item) => {
      const key = `${item.instance} / ${item.source}`;
      acc[key] = (acc[key] || 0) + 1;
      return acc;
    }, {});

    result.textContent = JSON.stringify({ summary, responses }, null, 2);
  } catch (error) {
    result.textContent = error.message;
  }
});

document.getElementById("search-btn").addEventListener("click", () => {
  const q = document.getElementById("search-input").value.trim();
  if (!q) {
    searchResult.textContent = "请输入关键词";
    return;
  }
  renderJson(searchResult, fetch(`/api/search?q=${encodeURIComponent(q)}`));
});

document.getElementById("search-input").addEventListener("keydown", (e) => {
  if (e.key === "Enter") {
    document.getElementById("search-btn").click();
  }
});

document.getElementById("rw-test").addEventListener("click", () => {
  renderJson(rwResult, fetch("/api/rw-test"));
});

document.getElementById("add-product").addEventListener("click", async () => {
  rwResult.textContent = "写入中...";
  try {
    const response = await fetch("/api/products", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        name: "New Product " + Date.now(),
        price: Math.floor(Math.random() * 100) + 10,
        stock: Math.floor(Math.random() * 50) + 1,
        description: "Created via frontend to test master write."
      })
    });
    const data = await response.json();
    rwResult.textContent = JSON.stringify(data, null, 2);
  } catch (error) {
    rwResult.textContent = error.message;
  }
});
