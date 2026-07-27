const capsule = document.querySelector(".capsule");

capsule?.addEventListener("click", () => {
  const expanded = capsule.getAttribute("aria-expanded") === "true";
  capsule.setAttribute("aria-expanded", String(!expanded));
});

const observer = new IntersectionObserver(
  (entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        entry.target.classList.add("is-visible");
        observer.unobserve(entry.target);
      }
    });
  },
  { threshold: 0.18 }
);

document.querySelectorAll(".reveal").forEach((element) => observer.observe(element));
