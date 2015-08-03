// WikiPathways,
// Java bots to generate RSSM file
// Copyright 2014-2015 BiGCaT Bioinformatics
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package org.wikipathways.bots;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.Collection;
import java.util.Properties;

import org.bridgedb.bio.DataSourceTxt;
import org.bridgedb.rdb.GdbProvider;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.pathvisio.core.debug.Logger;
import org.pathvisio.core.preferences.PreferenceManager;
import org.wikipathways.bots.utils.GenerateRSSM;

public class RSSMBot extends Bot {

	private static final String PROP_GDBS = "gdb-config";
	GdbProvider gdbs;
	
	public RSSMBot(Properties props) throws BotException {
		super(props);
		
		File gdbFile = new File(props.getProperty(PROP_GDBS));
		try {
			DataSourceTxt.init();
			gdbs = GdbProvider.fromConfigFile(gdbFile);
		} catch (Exception e) {
			throw new BotException(e);
		}
	}

	@Override
	public BotReport createReport(Collection<Result> result) {
		BotReport report = new BotReport(
				new String[] {
					"Entrez Gene ids", "PubChem ids"
				}
			);
		
		report.setTitle("RSSM file generation report");
		report.setDescription("RSSM bot creates rssm file");
		return null;
	}

	@Override
	public String getTagName() {
		return null;
	}

	@Override
	protected Result scanPathway(File pathwayFile) throws BotException {
		return null;
	}
	
	@Override
	public Boolean updateTags() {
		return false;
	}

	public static void main(String[] args) {
		try {
			Logger.log.trace("Starting RSSMBot");
			PreferenceManager.init();
			Properties props = new Properties();
			props.load(new FileInputStream(new File(args[0])));
			RSSMBot bot = new RSSMBot(props);
			File output = new File(args[1]);
			
			GenerateRSSM rssm = new GenerateRSSM(bot.getCache(), bot.getClient(), bot.gdbs);
			rssm.setSourceUrl("http://www.wikipathways.org");
			
			Document doc = rssm.createRSSM(bot.getCache().getFiles());
			XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
			FileWriter writer = new FileWriter(output);
			out.output(doc, writer);
			writer.flush();
			writer.close();
		} catch(Exception e) {
			e.printStackTrace();
			printUsage();
		}
	}

	static private void printUsage() {
		System.out.println(
			"Usage:\n" +
			"java org.pathvisio.wikipathways.bots.RSSMBot propsfile reportfilename\n" +
			"Where:\n" +
			"-propsfile: a properties file containing the bot properties\n" +
			"-reportfilename: the base name of the file that will be used to write reports to " +
			"(extension will be added automatically)\n"
		);
	}
}
