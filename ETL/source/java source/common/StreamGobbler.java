package com.dbs.sg.DTE12.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

public class StreamGobbler extends Thread
{
	/**
	 * Log4j Logger
	 */
	private Logger logger;

	InputStream is;
	String type;
	OutputStream os;
	Process proc;

	public StreamGobbler(String configPath, Process proc, InputStream is, String type)
	{
		this(configPath, proc, is, type, null);
	}

	public StreamGobbler(String configPath, Process proc, InputStream is, String type, OutputStream redirect)
	{
		this.is = is;
		this.type = type;
		this.os = redirect;
		logger= Logger.getLogger(configPath, StreamGobbler.class);
		this.proc = proc;
	}

	public void run()
	{
		try
		{
			PrintWriter pw = null;
			if (os != null)
			{
				pw = new PrintWriter(os);
			}

			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			while ((line = br.readLine()) != null)
			{
				if (pw != null)
				{
					pw.println(line);
				}
				logger.info(type + ">" + line);
				if (line.toLowerCase().startsWith("imp-")){
					this.proc.destroy();
					break;
				}
			}
			if (pw != null)
			{
				pw.flush();
			}
		}
		catch (IOException ioe)
		{
			// ioe.printStackTrace();
			logger.error("Error while reading the process output.",ioe);
		}
	}
}
