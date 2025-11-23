import zipfile
import tempfile
import shutil
import os
import string
import tkinter as tk
from tkinter import filedialog, messagebox

def concatText(text1, text2):
    """
    Merges two text files with the following rules:
    - Each file has a 1-line header.
    - The 2nd word in each header is a packet count.
    - The output keeps the first file’s header but updates the count.
    - The second file’s header is removed entirely.
    - Body of file 2 is appended after file 1.
    """

    # --- Split into lines ---
    lines1 = text1.splitlines()
    lines2 = text2.splitlines()

    # --- Extract headers ---
    header1 = lines1[0]
    header2 = lines2[0]

    # --- Extract packet count (2nd word) - this will be recalculated ---
    # We don't use these counts directly anymore, but extract the header structure

    # --- Get bodies (all lines after header) ---
    body1 = lines1[1:]
    
    # Extract the first effect from header2 (everything after "Pkt_count: #,")
    # The header format is: "Pkt_count: 78, Size: 0, Strip_id: 0, Set_id: 0..."
    header2_effect = None
    if "\n" in header2:
        parts = header2.split("\n", 1)  # Split only on first comma
        if len(parts) > 1:
            header2_effect = parts[1]  # Everything after "Pkt_count: #,"
    
    body2 = lines2[1:]   # Keep all effect lines from file 2

    # --- Find the maximum Set_id in file 1 (including header1's effect) ---
    max_set_id = -1
    for line in [header1] + body1:
        if "Set_id:" in line:
            parts = line.split("Set_id:")
            if len(parts) > 1:
                set_id_str = parts[1].split(",")[0].strip()
                try:
                    max_set_id = max(max_set_id, int(set_id_str))
                except ValueError:
                    pass

    # --- Increment Set_id values in body2 (and header2's effect) ---
    offset = max_set_id + 1
    
    # First, process the effect from header2
    body2_updated = []
    if header2_effect:
        line = header2_effect
        if "Set_id:" in line:
            parts = line.split("Set_id:")
            if len(parts) > 1:
                remaining = parts[1].split(",", 1)
                if len(remaining) > 1:
                    try:
                        old_set_id = int(remaining[0].strip())
                        new_set_id = old_set_id + offset
                        line = parts[0] + f"Set_id: {new_set_id}," + remaining[1]
                    except ValueError:
                        pass
        body2_updated.append(line)
    
    # Then process the rest of body2
    for line in body2:
        if "Set_id:" in line:
            parts = line.split("Set_id:")
            if len(parts) > 1:
                remaining = parts[1].split(",", 1)
                if len(remaining) > 1:
                    try:
                        old_set_id = int(remaining[0].strip())
                        new_set_id = old_set_id + offset
                        line = parts[0] + f"Set_id: {new_set_id}," + remaining[1]
                    except ValueError:
                        pass
        body2_updated.append(line)

    # --- Calculate actual packet count (all effects including header effects) ---
    # Count all effect lines: body1 + header2 effect (if exists) + body2
    total_packets = len(body1)  # Body1 effects
    if header2_effect:
        total_packets += 1  # Header2 effect
    total_packets += len(body2)  # Body2 effects
    
    # --- Replace the 2nd word in header1 with the correct count ---
    header1_parts = header1.split()
    header1_parts[1] = str(total_packets)
    new_header1 = " ".join(header1_parts)

    # --- Re-assemble the merged text ---
    newText = "\n".join([new_header1] + body1 + body2_updated)

    return newText

def concatenate_files():

    root = tk.Tk()
    root.withdraw()  # Hide main window

    # ----------------------------------
    # Select first ZIP
    # ----------------------------------
    zip1_path = filedialog.askopenfilename(
        title="Select FIRST pkt file",
        filetypes=[("Packet Files", "*.pkt"), ("ZIP Files", "*.zip"), ("All Files", "*.*")]
    )
    if not zip1_path:
        return

    # ----------------------------------
    # Select second ZIP
    # ----------------------------------
    zip2_path = filedialog.askopenfilename(
        title="Select SECOND pkt file",
        filetypes=[("Packet Files", "*.pkt"), ("ZIP Files", "*.zip"), ("All Files", "*.*")]
    )
    if not zip2_path:
        return

    # ----------------------------------
    # Create output folder name based on input files
    # ----------------------------------
    merged_name = "Combined Packets"
    
    # Save in same directory as first file
    parent_dir = os.path.dirname(zip1_path)
    final_pkt_path = os.path.join(parent_dir, merged_name + ".pkt")

    # ----------------------------------
    # Create temporary directories
    # ----------------------------------
    temp1 = tempfile.mkdtemp()
    temp2 = tempfile.mkdtemp()
    out_folder = tempfile.mkdtemp()  # Temporary output folder

    try:
        # Unzip both archives
        with zipfile.ZipFile(zip1_path, 'r') as z1:
            z1.extractall(temp1)
        with zipfile.ZipFile(zip2_path, 'r') as z2:
            z2.extractall(temp2)

        # Find matching files by integer filename
        files1 = set(os.listdir(temp1))
        files2 = set(os.listdir(temp2))

        def sort_key(filename):
            name_without_ext = os.path.splitext(filename)[0]
            try:
                return (0, int(name_without_ext))  # Numbers first, sorted numerically
            except ValueError:
                return (1, name_without_ext)  # Strings second, sorted alphabetically

        common = sorted(
            files1.intersection(files2),
            key=sort_key
        )

        for fname in common:
            try:
                if int(os.path.splitext(fname)[0]) % 10 == 0:
                    print(f"Merged [{fname}/{common[-1]}]")
            except ValueError:
                # For non-numeric filenames, print every file
                print(f"Merged [{fname}/{common[-1]}]")
            file1 = os.path.join(temp1, fname)
            file2 = os.path.join(temp2, fname)

            # Read files
            with open(file1, 'r', encoding='utf-8') as f:
                t1 = f.read()
            with open(file2, 'r', encoding='utf-8') as f:
                t2 = f.read()

            # Merge them
            merged_text = concatText(t1, t2)

            # Save merged output
            out_path = os.path.join(out_folder, fname)
            with open(out_path, 'w', encoding='utf-8') as f:
                f.write(merged_text)
        print("\nAll files merged. Creating final .pkt...\n")

        # ----------------------------------
        # Create final .pkt file (which is a ZIP with .pkt extension)
        # ----------------------------------
        with zipfile.ZipFile(final_pkt_path, 'w', zipfile.ZIP_DEFLATED) as zout:
            for fname in sorted(os.listdir(out_folder), key=sort_key):
                zout.write(os.path.join(out_folder, fname), fname)

        messagebox.showinfo(
            "Success",
            f"All files merged successfully!\n\nCreated Packet File:\n{final_pkt_path}"
        )

    finally:
        # Always clean up
        shutil.rmtree(temp1)
        shutil.rmtree(temp2)
        shutil.rmtree(out_folder)


if __name__ == "__main__":
    concatenate_files()

