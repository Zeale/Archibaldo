/**
 *
 */
package archibald.likes.packages.app;

import static archibald.likes.packages.api.utils.DiscordUtils.canAttachFile;
import static archibald.likes.packages.api.utils.DiscordUtils.canSendMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.alixia.javalibrary.JavaTools;
import org.alixia.javalibrary.strings.matching.Matching;

import archibald.likes.packages.Archibald;
import archibald.likes.packages.api.commands.BotCommandInvocation;
import archibald.likes.packages.api.commands.BotCommandInvocationParser;
import archibald.likes.packages.api.commands.BotCommandNamespace;
import archibald.likes.packages.api.commands.CommandNamespace;
import archibald.likes.packages.api.utils.DiscordUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * @author Zeale
 *
 */
public class PublicCommandHandler {

	private final BotCommandNamespace rootCommandNamespace = new BotCommandNamespace();

	{
		rootCommandNamespace.makeHelpCommand();

		// Add command help.
		rootCommandNamespace.addCommandHelp("hello", "Says hello to the invoker.", "hello", "hi");
		// Add actual command.
		rootCommandNamespace.new PublicCommand("hello", "hi") {

			@Override
			protected void run(BotCommandInvocation<MessageReceivedEvent> data) {
				// data.args is the array of arguments that the invoker provides.

				// If the invoker doesn't provide any arguments, reply "Hi." Otherwise, notify
				// them of invoking the command wrong.
				reply(data, data.args.length == 0 ? "Hi." : "That command doesn't take any arguments.");
			}
		};

		rootCommandNamespace.addCommandHelp("remind-me",
				"Schedules a reminder. The bot will ping you once the reminder goes up.",
				"remind-me (time-till) [description...]", "remind");
		rootCommandNamespace.new PublicCommand("remind", "remind-me") {

			@Override
			protected void run(BotCommandInvocation<MessageReceivedEvent> data) {
				if (data.args.length == 0) {
					reply(data, "You need to specify how long until I should remind you.");
					return;
				}

				long mil;
				try {
					mil = Long.parseLong(data.args[0].substring(0, data.args[0].length() - 1)) * 1000;
				} catch (NumberFormatException e) {
					reply(data,
							"Your first argument couldn't be parsed as an amount of time. Please make sure you're including a unit (one of either `s` (for seconds), `m` (for minutes), or `h` (for hours)).");
					return;
				}
				switch (data.args[0].charAt(data.args.length - 1)) {
				case 's':
					break;
				case 'h':
					mil *= 60;
				case 'm':
					mil *= 60;
					break;
				default:
					reply(data,
							"Invalid time unit. Allowable units are `s` (for seconds), `m` (for minutes), or `h` (for hours).");
					return;
				}

				if (data.args.length == 1) {
					new Timer(true).schedule(new TimerTask() {

						@Override
						public void run() {
							reply(data, "A reminder for: " + data.getData().getAuthor().getAsMention() + '.');
						}
					}, mil);
				} else {
					StringBuilder b = new StringBuilder();
					b.append(data.args[1]);
					for (int i = 2; i < data.args.length; i++)
						b.append(' ').append(data.args[i]);

					new Timer(true).schedule(new TimerTask() {

						@Override
						public void run() {
							reply(data,
									"Hey, " + data.getData().getAuthor().getAsMention() + ": `" + b.toString() + "`.");
						}
					}, mil);
				}

				reply(data, "Your reminder was scheduled successfully.");

			}
		};

		rootCommandNamespace.addCommandHelp("sort", "Sorts a list of Strings", "sort [args...]", "srt");
		rootCommandNamespace.new PublicCommand() {
			@Override
			protected void run(BotCommandInvocation<MessageReceivedEvent> data) {
				String[] args = data.args;
				Arrays.sort(args);
				reply(data, "[" + String.join(" ", args) + "]");
			}
		};
	}

	private static boolean canatchfile(BotCommandInvocation<MessageReceivedEvent> o) {
		return canAttachFile(o.getData().getChannel());
	}

	private static boolean cansendmsg(BotCommandInvocation<MessageReceivedEvent> o) {
		return canSendMessage(o.getData().getChannel());
	}

	private static final List<User> fetchUsers(String ref, BotCommandInvocation<MessageReceivedEvent> data,
			boolean ignoreCase) {
		User user;
		if (!ref.startsWith("\\"))
			try {
				if ((user = data.getData().getJDA().getUserById(ref.contains(">")
						? ref.substring(ref.startsWith("<@") ? ref.startsWith("<@!") ? 3 : 2 : 0, ref.indexOf(">"))
						: ref)) != null)
					return Collections.singletonList(user);
			} catch (final NumberFormatException e) {
			}
		else
			ref = ref.substring(1);
		if (ref.startsWith("#")) {
			final User u = data.getData().getJDA().getUserById(ref.substring(1));
			if (u != null)
				return Collections.singletonList(u);
		}
		if (ref.contains("#")) {
			final String refcut = ref.substring(0, ref.lastIndexOf('#'));
			if (!refcut.isEmpty())
				for (final User u : data.getData().getJDA().getUsersByName(refcut, ignoreCase))
					if (u.getDiscriminator().equals(ref.substring(ref.lastIndexOf('#') + 1)))
						return Collections.singletonList(u);
		}

		final List<User> plausibleUsers = new LinkedList<>();
		if (data.getData().isFromGuild()) {
			for (final Member m : data.getData().getGuild().getMembers()) {
				final String displayName = m.getEffectiveName();
				if (ignoreCase ? displayName.equalsIgnoreCase(ref) : displayName.equals(ref))
					plausibleUsers.add(m.getUser());
			}
			if (!plausibleUsers.isEmpty())
				return plausibleUsers;

			for (final Member m : data.getData().getGuild().getMembers()) {
				final String name = m.getUser().getName();
				if (ignoreCase ? name.equalsIgnoreCase(ref) : name.equals(ref))
					plausibleUsers.add(m.getUser());
			}
			if (!plausibleUsers.isEmpty())
				return plausibleUsers;

		}
		return data.getData().getJDA().getUsersByName(ref, ignoreCase);

	}

	private static final java.awt.Color getRandomSaturatedColor_AWT() {
		return java.awt.Color.getHSBColor((float) Math.random(), 1, 1);
	}

	/**
	 * <p>
	 * Returns a {@link List} of {@link VoiceChannel}s that match the given
	 * {@link String} reference.
	 * </p>
	 * <p>
	 * If the reference starts with a backslash (<code>\</code>), the backslash is
	 * disregarded. Otherwise, the presence of a hashtag/pound symbol
	 * (<code>#</code>) in the first-character position is checked. If the pound
	 * symbol is found, it is disregarded and the remainder of the reference is
	 * checked to be the ID of a voice channel. If it isn't,
	 *
	 * @param ref
	 * @param guild
	 * @return
	 */
	private static List<VoiceChannel> getVCByReference(String ref, Guild guild) {
		if (ref.startsWith("\\"))
			ref = ref.substring(1);
		else if (ref.startsWith("#")) {
			final VoiceChannel vc = guild.getVoiceChannelById(ref.substring(1));
			if (vc != null)
				return Collections.singletonList(vc);
		}
		final List<VoiceChannel> channels = guild.getVoiceChannelsByName(ref, true);
		if (!(channels.isEmpty() || channels.size() == 1))
			for (final Iterator<VoiceChannel> iterator = channels.iterator(); iterator.hasNext();)
				if (!iterator.next().getName().equals(ref))
					iterator.remove();
		return channels;
	}

	/**
	 * <p>
	 * Various types of user references are ideated by this method.
	 * </p>
	 * <h2>Reference Types</h2>
	 * <p>
	 * <ol>
	 * <li><b>ID References</b> - These are references composed solely of a users
	 * ID.</li>
	 * <li><b>Absolute References</b> - These are references composed of a username
	 * followed by a <code>#</code> and a discriminator, in that order. These can
	 * possibly be prepended with a Guild reference and then a <code>:</code>, and
	 * then possibly a channel reference, followed by another <code>:</code>.</li>
	 * <li><b>Simple References</b> - These are vague references used to refer to a
	 * user. They can be made more specific by being prepended with a Guild
	 * reference, and then a <code>:</code>, followed by a channel reference, and
	 * then a <code>:</code>. The channel reference and its following colon can be
	 * individually left out, as can the guild reference and its immediately
	 * following colon.</li>
	 * </ol>
	 * <h2>Parsing</h2>
	 * <p>
	 * Reference checking begins simple. If the input ref starts with a
	 * <code>#</code> then it is assumed to be an exact user id. The <code>#</code>
	 * is subsequently removed and the remainder of the reference is parsed as a
	 * long. If this parsing fails or a user isn't found by the given ID, the input
	 * is assumed to be a nickname reference. TODO
	 * </p>
	 * <p>
	 * If the input ref does not start with <code>#</code>, it is first parsed as an
	 * ID
	 *
	 * @param ref
	 * @param client
	 * @param channel The channel to be involved in the name search. This may be
	 *                null.
	 * @return
	 */
	private static List<User> listUsers(String ref, MessageChannel channel) {
		final JDA client = channel.getJDA();
		if (ref.startsWith("#"))
			try {
				// If this fails, assume nickname, or guildname then possibly channel name then
				// possibly username. (Can't be a username alone.)
				final User user = client.getUserById(Long.parseLong(ref.substring(1)));
				if (user != null)
					return Collections.singletonList(user);
			} catch (final NumberFormatException e) {
			}
		else if (!ref.contains(":"))
			// Username['#'discriminator]
			if (ref.contains("#")) {
				final String id = ref.substring(ref.indexOf('#') + 1, ref.length());
				for (final User u : DiscordUtils.getUsersByName(ref.substring(0, ref.indexOf('#')), true, client))
					if (u.getDiscriminator().equals(id))
						return Collections.singletonList(u);
			} else {
				final List<User> users = DiscordUtils.getUsersByName(ref, true, client);
				if (!users.isEmpty())
					return users;
			}

		// ID/~~((Guild':'|Channel':')|Guild':'Channel':')~~(Username|Nickname)

		if (ref.startsWith("<@") && ref.endsWith(">")) {
			String tag = ref.substring(2, ref.length() - 1);
			if (tag.startsWith("!"))
				tag = tag.substring(1);
			try {
				final User user = client.getUserById(Long.parseLong(tag));
				if (user != null)
					return Collections.singletonList(user);
			} catch (final NumberFormatException e) {
			}
		}

		// ID
		try {
			final User user = client.getUserById(Long.parseLong(ref));
			if (user != null)
				return Collections.singletonList(user);
		} catch (final NumberFormatException e) {
		}

		// Username
		if (channel instanceof GuildChannel) {// OPTIMIZE
			final List<User> users = new ArrayList<>(1), nickMatches = new ArrayList<>(3);
			final GuildChannel txtChannel = (GuildChannel) channel;
			for (final Member u : txtChannel.getMembers())
				if (DiscordUtils.fixStringEncoding(u.getEffectiveName()).equalsIgnoreCase(ref))
					users.add(u.getUser());
				else {
					final String nick = DiscordUtils.fixStringEncoding(u.getEffectiveName());
					if (nick != null && nick.equalsIgnoreCase(ref))
						nickMatches.add(u.getUser());
				}
			return users.isEmpty() ? nickMatches : users;
		}
		return Collections.emptyList();
	}

	private static int parseInteger(String input) throws NumberFormatException {
		input = input.toLowerCase();

		boolean n = false;
		if (input.startsWith("-")) {
			input = input.substring(1);
			n = true;
		} else if (input.startsWith("+"))
			input = input.substring(1);

		int res;
		res = input.startsWith("0x") ? Integer.decode(input)
				: input.startsWith("0b") ? Integer.parseInt(input.substring(2), 2)
						: input.startsWith("0o") ? Integer.parseInt(input.substring(2), 8)
								: input.startsWith("0") ? Integer.parseInt(input.substring(1), 8)
										: Integer.parseInt(input);
		if (n)
			res = -res;

		return res;
	}

	private static final void printIcon(User user, BotCommandInvocation<MessageReceivedEvent> data) {
		data.getData().getChannel()
				.sendMessage(new EmbedBuilder().setColor(getRandomSaturatedColor_AWT())
						.setImage(user.getAvatarUrl().concat("?size=1024"))
						.setTitle(user.getName() + "'s profile picture:").build())
				.queue();
	}

	private static final boolean printIcons(Collection<? extends User> users,
			BotCommandInvocation<MessageReceivedEvent> data) {
		for (final User u : users)
			printIcon(u, data);
		return !users.isEmpty();
	}

	private static String printInEnglish(Iterator<? extends Object> itr, boolean and) {
		final StringBuilder builder = new StringBuilder();
		if (itr.hasNext()) {
			builder.append(itr.next());
			if (itr.hasNext()) {
				Object o = itr.next();
				if (!itr.hasNext())
					builder.append(' ' + (and ? "and" : "or") + ' ').append(o);
				else {
					Object two = itr.next();
					while (itr.hasNext()) {
						builder.append(", ").append(o);
						o = two;
						two = itr.next();
					}
					builder.append(", ").append(o).append(", " + (and ? "and" : "or") + ' ').append(two);
				}
			}
		}
		return builder.toString();
	}

	private Matching cmdMatching;

	private final BotCommandInvocationParser parser = new BotCommandInvocationParser("");

	private final Archibald instance;

	public PublicCommandHandler(Matching cmdMatching, Archibald inst) {
		setCmdMatching(cmdMatching);
		instance = inst;
	}

	/**
	 * Sends the specified text in the channel specified in the <code>data</code>'s
	 * {@link MessageReceivedEvent}, if possible. If this method fails due to a lack
	 * of permissions to send messages in a {@link GuildChannel}, or fails due to
	 * sharing no guilds with the recipient of a private channel, it returns
	 * <code>false</code>. Otherwise, it returns <code>true</code> (assuming it
	 * completes normally).
	 *
	 * @param data The data containing the {@link MessageReceivedEvent} containing
	 *             the {@link MessageChannel} to send messages in.
	 * @param text The text to send.
	 * @return <code>false</code> due to a lack of perms in a {@link GuildChannel}
	 *         or due to inability to send in a private message, or
	 *         <code>true</code> otherwise.
	 */
	private boolean reply(BotCommandInvocation<MessageReceivedEvent> data, String text) {
		return reply(data.getData(), text);
	}

	/**
	 * Sends the specified message in the channel that the
	 * {@link MessageReceivedEvent} happened in.
	 *
	 * @param event the {@link MessageReceivedEvent}.
	 * @param text  the reply.
	 */
	private boolean reply(MessageReceivedEvent event, String text) {
		if (!canSendMessage(event.getChannel()))
			return false;
		event.getChannel().sendMessage(text).queue();
		return true;
	}

	private final Map<String, String[]> namespaceCache = new HashMap<>();

	public boolean run(MessageReceivedEvent event) {
		final String res = cmdMatching.match(event.getMessage().getContentRaw());
		if (res.length() != event.getMessage().getContentRaw().length()) {
			final BotCommandInvocation<MessageReceivedEvent> r = parser.parse(res, event);
			CommandNamespace<BotCommandInvocation<MessageReceivedEvent>, String> ns;
			if (r.namespaces != null && r.namespaces.length != 0) {
				if (r.namespaces[0].isEmpty() && r.namespaces.length == 1)
					ns = rootCommandNamespace;
				else if ((ns = rootCommandNamespace.getSubNamespace(
						JavaTools.mask(JavaTools.iterable(r.namespaces), a -> a.toLowerCase()))) == null) {
					reply(event, "Couldn't find a namespace that matches that specified in your command invocation: (`"
							+ String.join(":", r.namespaces) + "`).");
					return true;
				}
			} else {
				final String[] cns = namespaceCache.get(event.getAuthor().getId());
				if (cns != null && cns.length != 0) {
					if ((ns = rootCommandNamespace
							.getSubNamespace(JavaTools.mask(JavaTools.iterable(cns), a -> a.toLowerCase()))) == null) {
						System.err.println("A stored namespace was invalid: " + String.join(":", cns) + '.');
						ns = rootCommandNamespace;
					}
				} else
					ns = rootCommandNamespace;
			}
			return ns.run(r);
		}
		return false;
	}

	public void setCmdMatching(Matching cmdMatching) {
		if (cmdMatching == null)
			throw new IllegalArgumentException("Command matching cannot be null.");
		this.cmdMatching = cmdMatching;
	}

}
