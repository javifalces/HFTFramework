from os import path, makedirs

def save_model(model, model_name, summary=True, h5_path = "output_h5", 
                                        summary_path = "output_summary"):
    makedirs(h5_path, exist_ok=True)
    makedirs(summary_path, exist_ok=True)

    # # Save the model weights and config in a single file
    model.save(path.join(h5_path, model_name + ".h5"))

    with open(path.join(summary_path, model_name + ".txt"), mode='w') as f:
        model.summary(print_fn= lambda x: f.write(x + "\n"))


