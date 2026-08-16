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
