import util
import sys

CONFIGURATION_KEYS = {"letor_txt_files", "output_path"}


def merge(output_path, files):
    with open(output_path, "w") as out:
        for file in files:
            with open(file, "r") as i:
                for line in i:
                    out.write(line)


if __name__ == '__main__':
    if len(sys.argv) != 2:
        print("Usage: argv0 argv1")
        print("argv1: Configuration json file")
        sys.exit(2)
    else:
        validation, conf = util.read_configuration_json(sys.argv[1], CONFIGURATION_KEYS)

        if validation:
            merge(output_path=conf["output_path"], files=conf["letor_txt_files"])
        else:
            print("Configuration file cannot be validated, following keys must be satisfied.")
            print(CONFIGURATION_KEYS)
