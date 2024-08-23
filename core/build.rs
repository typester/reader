fn main() {
    // Uniffi
    uniffi::generate_scaffolding("src/manga.udl").unwrap();

    // Rebuild code if files in db/migrations were changed.
    println!("cargo:rerun-if-changed=db/migrations");
}
