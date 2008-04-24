/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.serverpackets;

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.network.L2GameClient;

import org.mmocore.network.SendablePacket;

/**
 *
 * @author  KenM
 */
public abstract class L2GameServerPacket extends SendablePacket<L2GameClient>
{
	private static final Logger _log = Logger.getLogger(L2GameServerPacket.class.getName());

	/**
	 * @see com.l2jserver.mmocore.network.SendablePacket#write()
	 */
	@Override
	protected void write()
	{
		try
		{
            //_log.info(this.getType());
			writeImpl();
		}
		catch (Throwable t)
		{
			_log.severe("Client: "+getClient().toString()+" - Failed writing: "+getType()+" - L2J Server Version: "+Config.SERVER_VERSION+" - DP Revision: "+Config.DATAPACK_VERSION);
			t.printStackTrace();
		}
	}

	public void runImpl()
	{

	}

	protected abstract void writeImpl();

	/**
	 * @return A String with this packet name for debuging purposes
	 */
	public abstract String getType();

    /**
     * @see org.mmocore.network.SendablePacket#getHeaderSize()
     */
    @Override
    protected int getHeaderSize()
    {
        return 2;
    }

    /**
     * @see org.mmocore.network.SendablePacket#writeHeader(int)
     */
    @Override
    protected void writeHeader(int dataSize)
    {
        writeH(dataSize + this.getHeaderSize());
    }
}
