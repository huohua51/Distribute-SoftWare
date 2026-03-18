const result = document.getElementById("result");
const healthResult = document.getElementById("health-result");

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
      Array.from({ length: 20 }, () => fetch("/api/products/1").then((res) => res.json()))
    );

    const summary = responses.reduce((acc, item) => {
      const key = `${item.instance} / ${item.source}`;
      acc[key] = (acc[key] || 0) + 1;
      return acc;
    }, {});

    result.textContent = JSON.stringify(
      {
        summary,
        responses
      },
      null,
      2
    );
  } catch (error) {
    result.textContent = error.message;
  }
});
