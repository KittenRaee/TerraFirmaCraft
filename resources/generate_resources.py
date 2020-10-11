#  Work under Copyright. Licensed under the EUPL.
#  See the project README.md and LICENSE.txt for more information.

# Script to run all resource generation

from mcresources import ResourceManager

import assets
import collapse_recipes
import data
import ore_veins
import recipes
import world_gen
from constants import *


def main():
    rm = ResourceManager('tfc', resource_dir='../src/main/resources')
    # clean_generated_resources('/'.join(rm.resource_dir))

    # do simple lang keys first, because it's ordered intentionally
    rm.lang(DEFAULT_LANG)

    # generic assets / data
    assets.generate(rm)
    data.generate(rm)
    world_gen.generate(rm)
    recipes.generate(rm)

    # more complex stuff n things
    ore_veins.generate(rm)
    collapse_recipes.generate(rm)

    rm.flush()

    print('New = %d, Modified = %d, Unchanged = %d, Errors = %d' % (rm.new_files, rm.modified_files, rm.unchanged_files, rm.error_files))


if __name__ == '__main__':
    main()
