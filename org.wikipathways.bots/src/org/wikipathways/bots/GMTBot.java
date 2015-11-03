// WikiPathways,
// Java bots to generate GMT file
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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import org.bridgedb.bio.DataSourceTxt;
import org.bridgedb.rdb.GdbProvider;
import org.pathvisio.core.debug.Logger;
import org.pathvisio.core.preferences.PreferenceManager;
import org.wikipathways.bots.utils.GenerateGMT;

/**
 * Bot creates a GMT file 
 * Gene Set file format
 * @author martina
 *
 */
public class GMTBot extends Bot {

	private static final String PROP_GDBS = "gdb-config";
	GdbProvider gdbs;
	
	public GMTBot(Properties props) throws BotException {
		super(props);
		
		File gdbFile = new File(props.getProperty(PROP_GDBS));
		System.out.println(gdbFile.exists());
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
					"Entrez Gene ids"
				}
			);
		
		report.setTitle("GMT file generation report");
		report.setDescription("GMT bot creates GMT file");
		return null;
	}

	@Override
	public String getTagName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Result scanPathway(File pathwayFile) throws BotException {
		// TODO Auto-generated method stub
		return null;
	}
	
	public static void main(String[] args) {
		try {
			Logger.log.trace("Starting GMTBot");
			PreferenceManager.init();
			Properties props = new Properties();
			props.load(new FileInputStream(new File(args[0])));
			GMTBot bot =  new GMTBot(props);
			bot.getCache().update();

			File output = new File(args[1]);
			
			GenerateGMT gmt = new GenerateGMT(bot.gdbs, bot.getClient());
			
			Calendar cal = Calendar.getInstance();
	        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
	        String date = sdf.format(cal.getTime());
			
	        String syscode = args[2];
	        
			Map<String, String> res = gmt.createGMTFile(bot.getCache().getFiles(), date, syscode);
//			String result = gmt.createGMTFile(bot.getCache().getFiles());
			
			for(String s : res.keySet()) {
				File f = new File(output, s);
				f.mkdir();
				File out = new File(f, "wikipathways_" + syscode + "_" + date + ".gmt");
				FileWriter writer = new FileWriter(out);
				writer.write(res.get(s));
				writer.close();
				
				File out2 = new File(f, "wikipathways_" + syscode + ".gmt");
				FileWriter writer2 = new FileWriter(out2);
				writer2.write(res.get(s));
				writer2.close();
			}
		} catch(Exception e) {
			e.printStackTrace();
			printUsage();
		}
	}

	static private void printUsage() {
		System.out.println(
			"Usage:\n" +
			"java org.pathvisio.wikipathways.bots.GMTBot propsfile reportfilename\n" +
			"Where:\n" +
			"-propsfile: a properties file containing the bot properties\n" +
			"-reportfilename: the base name of the file that will be used to write reports to " +
			"(extension will be added automatically)\n"
		);
	}
}
