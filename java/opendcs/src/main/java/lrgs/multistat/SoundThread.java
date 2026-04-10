/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package lrgs.multistat;

import java.io.File;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Line;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

public class SoundThread extends Thread implements LineListener
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private File soundFile = null;
	private long pauseMsec = 0;
	private int numTimes = 0;
	private Clip clip;
	private long clipLoadTime = 0L;

	private static final int IDLE = 0;
	private static final int PLAYING = 1;
	private static final int PAUSING = 2;
	private static final int SHUTDOWN = 3;
	private int state = IDLE;

	public SoundThread()
	{
		shutup();
	}

	public void playOnce(File soundFile)
	{
		playMulti(soundFile, 0L, 1);
	}

	public synchronized void
		playMulti(File soundFile, long pauseMsec, int numTimes)
	{
		shutup();
		this.soundFile = soundFile;
		this.pauseMsec = pauseMsec;
		this.numTimes = numTimes;
	}

	public synchronized void shutup()
	{
		soundFile = null;
		pauseMsec = 0L;
		numTimes = 0;
		if (clip != null)
		{
			if (state == PLAYING)
				clip.stop();
			clip.close();
			clip = null;
		}
		clipLoadTime = 0L;
		state = IDLE;
	}

	public void run()
	{
		while (state != SHUTDOWN)
		{
			if (state == PLAYING)
			{
				try { sleep(50L); } catch (InterruptedException ex) {}
			}
			else if (state == PAUSING)
			{
				try { if (pauseMsec > 0) sleep(pauseMsec); }
				catch (InterruptedException ex) {}
				if (--numTimes <= 0)
					shutup();
				state = IDLE;
			}
			else if (soundFile != null)
			{
				play();
			}
			else
			{
				try { sleep(100L); } catch (InterruptedException ex) {}
			}
		}
	}

	public void shutdown()
	{
		shutup();
		state = SHUTDOWN;
	}

	/** Plays the sound once. */
	private synchronized void play()
	{
		try
		{
			if (clip == null || soundFile.lastModified() > clipLoadTime)
			{
				if (clip != null)
				{
					clip.close();
					clip = null;
				}
				Line.Info linfo = new Line.Info(Clip.class);
				Line line = AudioSystem.getLine(linfo);
				clip = (Clip) line;
				clip.addLineListener(this);
				AudioInputStream ais = AudioSystem.getAudioInputStream(
					soundFile);
				clip.open(ais);
			}
			state = PLAYING;
			clip.start();
		}
		catch (Exception ex)
		{
			log.atWarn().setCause(ex).log("Sound file exception.");
			clip = null;
		}
	}

	public void update(LineEvent le)
	{
		LineEvent.Type type = le.getType();
		if (type == LineEvent.Type.OPEN)
		{
			/** do nothing */
		}
		else if (type == LineEvent.Type.CLOSE)
		{
			/** do nothing */
		}
		else if (type == LineEvent.Type.START)
		{
			/** do nothing */
		}
		else if (type == LineEvent.Type.STOP)
		{
			if (state == PLAYING)
			{
				state = PAUSING;
			}
		}
	}
}