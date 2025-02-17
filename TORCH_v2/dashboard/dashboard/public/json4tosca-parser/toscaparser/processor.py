#    Licensed under the Apache License, Version 2.0 (the "License"); you may
#    not use this file except in compliance with the License. You may obtain
#    a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
#    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
#    License for the specific language governing permissions and limitations
#    under the License.


import argparse
import os
import sys

import base64
from io import BytesIO
import zipfile

from toscaparser.tosca_template import ToscaTemplate
from toscaparser.utils.gettextutils import _
from toscaparser.torch_parser import ConfigFileUtility
from toscaparser.template_processor import processTemplate, printProcessedTemplate
import toscaparser.utils.urlutils

#import toscaparser.template_printer

"""
TOSCA processor endpoint for the TORCH orchestrator

It can be used as,
#torch-tosca-processor --template-file=<path to the YAML template>
#torch-tosca-processor --template-file=<path to the CSAR zip file>
#torch-tosca-processor --template-file=<URL to the template or CSAR>

e.g.
#torch-tosca-processor
 --template-file=toscaparser/tests/data/tosca_helloworld.yaml
#torch-tosca-processor
 --template-file=toscaparser/tests/data/CSAR/csar_hello_world.zip
"""



class ProcessorParserShell(object):

    def get_parser(self, argv):
        parser = argparse.ArgumentParser(prog="torch-tosca-processor")

        parser.add_argument('--template-file',
                            metavar='<filename>',
                            required=True,
                            help=_('YAML template or CSAR file to parse.'))
        parser.add_argument('--template-name',
                            metavar='<templatename>',
                            required=True,
                            help=_('Shortname for the template.'))

        return parser

    def main(self, argv):
        parser = self.get_parser(argv)
        (args, extra_args) = parser.parse_known_args(argv)
        path = args.template_file
        name = args.template_name
        if os.path.isfile(path):
            self.parse(path, name)
        elif toscaparser.utils.urlutils.UrlUtils.validate_url(path):
            self.parse(path, name, False)
        else:
            # For BytesIO string of a ZIP
            try:
                decoded = base64.b64decode(path.encode());
                fp = BytesIO(decoded)
                try:
                    zipfile.ZipFile(fp)
                    self.parse(fp, name)
                except:
                    self.parse(decoded, name)
            except:
                raise ValueError(_('"%(path)s" is not a valid file.')
                             % {'path': path})

    def parse(self, path, name, a_file=True):
        tosca = ToscaTemplate(path, None, a_file)
        processed_tosca = processTemplate(tosca)
        #printProcessedTemplate(processed_tosca)
        parser = ConfigFileUtility()
        parser.generate_json(tosca, processed_tosca, name)
        #print("-------------------------PARSING DONE----------------------------------")

def processToscaTemplate(args=None):
    if args is None:
        args = sys.argv[1:]
    ProcessorParserShell().main(args)


def main(args=None):
    if args is None:
        args = sys.argv[1:]
    ProcessorParserShell().main(args)


if __name__ == '__main__':
    main()

