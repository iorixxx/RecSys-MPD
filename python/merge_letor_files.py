import argparse


CLI = argparse.ArgumentParser()

CLI.add_argument("output", help="Absolute path of the output txt file")
CLI.add_argument("--letors", help="Absolute paths of letor txt files to be merged", nargs="+", required=True)


def merge(output_path, files):
    with open(output_path, "w") as out:
        for file in files:
            with open(file, "r") as i:
                for line in i:
                    out.write(line)

    print("Files are merged into: %s" % output_path)


if __name__ == '__main__':
    args = CLI.parse_args()

    merge(output_path=args.output, files=args.letors)
