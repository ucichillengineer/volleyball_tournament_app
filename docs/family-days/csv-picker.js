window.chooseFamilyDaysCsv = (onSelected) => {
  const input = document.createElement("input");
  input.type = "file";
  input.accept = ".csv,text/csv";
  input.addEventListener("change", () => {
    const file = input.files && input.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.addEventListener("load", () => onSelected(reader.result));
    reader.readAsText(file);
  });
  input.click();
};

window.shareFamilyDaysGreeting = (greeting) => {
  window.open(`https://wa.me/?text=${encodeURIComponent(greeting)}`, "_blank", "noopener,noreferrer");
};

const familyDaysDatabase = () =>
  new Promise((resolve, reject) => {
    const request = indexedDB.open("family-days", 1);
    request.onupgradeneeded = () => request.result.createObjectStore("settings");
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });

window.loadFamilyDaysEvents = async (onLoaded) => {
  try {
    const database = await familyDaysDatabase();
    const request = database.transaction("settings", "readonly").objectStore("settings").get("events");
    request.onsuccess = () => onLoaded(request.result || null);
    request.onerror = () => onLoaded(null);
  } catch {
    onLoaded(null);
  }
};

window.saveFamilyDaysEvents = async (csv) => {
  const database = await familyDaysDatabase();
  database.transaction("settings", "readwrite").objectStore("settings").put(csv, "events");
};
