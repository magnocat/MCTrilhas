package com.magnocat.mctrilhas.land;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class LandManager {

    private final MCTrilhasPlugin plugin;
    private final Map<UUID, Location> pos1 = new HashMap<>();
    private final Map<UUID, Location> pos2 = new HashMap<>();
    private final Map<UUID, Long> homeCooldowns = new HashMap<>();
    private final Map<UUID, SaleOffer> saleOffers = new HashMap<>(); // Key: Buyer UUID

    private record SaleOffer(UUID seller, double price, String regionId) {}

    public LandManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    public void setPos1(Player player, Location location) {
        pos1.put(player.getUniqueId(), location);
        player.sendMessage(ChatColor.GREEN + "Posição 1 definida! (Clique esquerdo)");
        if (!pos2.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "Agora clique com o botão direito em outro bloco para definir a Posição 2.");
        } else {
            player.sendMessage(ChatColor.AQUA + "Área selecionada! Use /terreno custo ou /terreno reivindicar.");
        }
    }

    public void setPos2(Player player, Location location) {
        pos2.put(player.getUniqueId(), location);
        player.sendMessage(ChatColor.GREEN + "Posição 2 definida! (Clique direito)");
        if (!pos1.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "Agora clique com o botão esquerdo em outro bloco para definir a Posição 1.");
        } else {
            player.sendMessage(ChatColor.AQUA + "Área selecionada! Use /terreno custo ou /terreno reivindicar.");
        }
    }

    public void showClaimCost(Player player) {
        Location p1 = pos1.get(player.getUniqueId());
        Location p2 = pos2.get(player.getUniqueId());

        if (p1 == null || p2 == null) {
            player.sendMessage(ChatColor.RED + "Você precisa definir as duas posições com a pá de ouro para ver o custo.");
            return;
        }

        // --- Calculation ---
        int sideX = Math.abs(p1.getBlockX() - p2.getBlockX()) + 1;
        int sideZ = Math.abs(p1.getBlockZ() - p2.getBlockZ()) + 1;
        long totalBlocks = (long) sideX * sideZ;

        double costPerBlock = plugin.getConfig().getDouble("land-protection.cost.per-block", 10.0);
        double totalCost = totalBlocks * costPerBlock;

        player.sendMessage(ChatColor.GOLD + "--- Custo da Seleção ---");
        player.sendMessage(ChatColor.AQUA + "Tamanho: " + ChatColor.WHITE + sideX + "x" + sideZ + " (" + totalBlocks + " blocos)");
        if (totalCost > 0) {
            player.sendMessage(ChatColor.AQUA + "Custo total: " + ChatColor.WHITE + String.format("%,.0f", totalCost) + " Totens");
        } else {
            player.sendMessage(ChatColor.AQUA + "Custo: " + ChatColor.GREEN + "Gratuito!");
        }
        player.sendMessage(ChatColor.GOLD + "-------------------------");
    }

    public void showLandInfo(Player player) {
        RegionManager regions = getRegionManager(player.getWorld());
        if (regions == null) {
            player.sendMessage(ChatColor.RED + "A proteção de terrenos não está habilitada neste mundo.");
            return;
        }

        Optional<ProtectedRegion> ownedRegion = findOwnedRegion(player);

        if (ownedRegion.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Você ainda não possui um terreno protegido.");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "--- Informações do seu Terreno ---");
        player.sendMessage(ChatColor.AQUA + "Dono: " + ChatColor.WHITE + player.getName());

        ProtectedRegion region = ownedRegion.get();
        player.sendMessage(ChatColor.AQUA + "ID do Terreno: " + ChatColor.WHITE + region.getId());
        Set<UUID> memberUuids = region.getMembers().getUniqueIds();
        if (memberUuids.isEmpty()) {
            player.sendMessage(ChatColor.AQUA + "Membros: " + ChatColor.GRAY + "Nenhum");
        } else {
            player.sendMessage(ChatColor.AQUA + "Membros:");
            for (UUID memberUuid : memberUuids) {
                OfflinePlayer member = Bukkit.getOfflinePlayer(memberUuid);
                player.sendMessage(ChatColor.GRAY + " - " + ChatColor.WHITE + member.getName());
            }
        }

        player.sendMessage(" ");
        player.sendMessage(ChatColor.GRAY + "Use /terreno add <jogador> para adicionar um amigo.");
        player.sendMessage(ChatColor.GOLD + "----------------------------------");
    }

    public void createClaim(Player player) {
        Location p1 = pos1.get(player.getUniqueId());
        Location p2 = pos2.get(player.getUniqueId());

        if (p1 == null || p2 == null) {
            player.sendMessage(ChatColor.RED + "Você precisa definir as duas posições com a pá de ouro antes de reivindicar.");
            return;
        }

        // --- Validação de Tamanho do Terreno ---
        int sideX = Math.abs(p1.getBlockX() - p2.getBlockX()) + 1;
        int sideZ = Math.abs(p1.getBlockZ() - p2.getBlockZ()) + 1;
        long totalBlocks = (long) sideX * sideZ;

        int maxTotalBlocks = plugin.getConfig().getInt("land-protection.size-limits.max-total-blocks", 10000);
        int maxSideLength = plugin.getConfig().getInt("land-protection.size-limits.max-side-length", 200);

        if (sideX > maxSideLength || sideZ > maxSideLength) {
            player.sendMessage(ChatColor.RED + "O terreno é muito grande! O comprimento máximo de cada lado é " + maxSideLength + " blocos.");
            player.sendMessage(ChatColor.YELLOW + "Sua seleção tem " + sideX + "x" + sideZ + " blocos.");
            return;
        }

        if (totalBlocks > maxTotalBlocks) {
            player.sendMessage(ChatColor.RED + "O terreno é muito grande! O limite total é de " + maxTotalBlocks + " blocos.");
            player.sendMessage(ChatColor.YELLOW + "Sua seleção tem " + totalBlocks + " blocos no total.");
            return;
        }

        RegionManager regions = getRegionManager(player.getWorld());
        if (regions == null) {
            player.sendMessage(ChatColor.RED + "A proteção de terrenos não está habilitada neste mundo.");
            return;
        }

        // --- Validação de Limite de Terrenos ---
        int maxClaims = plugin.getConfig().getInt("land-protection.max-claims-per-player", 1);
        long ownedRegionsCount = regions.getRegions().values().stream()
                .filter(r -> r.getOwners().contains(player.getUniqueId()))
                .count();

        if (ownedRegionsCount >= maxClaims) {
            player.sendMessage(ChatColor.RED + "Você atingiu o limite de " + maxClaims + " terreno(s) protegido(s).");
            return;
        }

        // Gera um ID único para a nova região
        String regionId = "terreno_" + player.getName().toLowerCase() + "_" + (ownedRegionsCount + 1);

        // --- Validação de Custo e Cobrança ---
        Economy econ = plugin.getEconomy();
        double costPerBlock = plugin.getConfig().getDouble("land-protection.cost.per-block", 10.0);
        double totalCost = totalBlocks * costPerBlock;

        if (econ == null && totalCost > 0) {
            player.sendMessage(ChatColor.RED + "O sistema de economia não está funcionando. Contate um administrador.");
            return;
        }

        if (totalCost > 0) {
            if (econ.getBalance(player) < totalCost) {
                player.sendMessage(ChatColor.RED + "Você não tem Totens suficientes para proteger este terreno.");
                player.sendMessage(ChatColor.YELLOW + "Custo total: " + String.format("%,.0f", totalCost) + " Totens (" + totalBlocks + " blocos x " + costPerBlock + " Totens/bloco).");
                return;
            }
            EconomyResponse r = econ.withdrawPlayer(player, totalCost);
            if (!r.transactionSuccess()) {
                player.sendMessage(ChatColor.RED + "Ocorreu um erro ao processar o pagamento. Tente novamente.");
                return;
            }
        }

        // Cria a região cubóide
        ProtectedCuboidRegion newRegion = new ProtectedCuboidRegion(regionId, BukkitAdapter.asBlockVector(p1), BukkitAdapter.asBlockVector(p2));

        // Define o jogador como dono da região
        DefaultDomain owners = newRegion.getOwners();
        owners.addPlayer(player.getUniqueId());
        newRegion.setOwners(owners);

        // Define as permissões (flags) padrão
        newRegion.setFlag(Flags.GREET_MESSAGE, ChatColor.AQUA + "Você está entrando no terreno de " + player.getName());
        newRegion.setFlag(Flags.FAREWELL_MESSAGE, ChatColor.YELLOW + "Você está saindo do terreno de " + player.getName());
        newRegion.setFlag(Flags.PVP, StateFlag.State.DENY); // Impede PVP

        regions.addRegion(newRegion);
        if (totalCost > 0) {
            player.sendMessage(ChatColor.GOLD + "Parabéns! Você gastou " + String.format("%,.0f", totalCost) + " Totens e seu terreno foi protegido!");
        } else {
            player.sendMessage(ChatColor.GOLD + "Parabéns! Seu terreno foi protegido com sucesso!");
        }
    }

    public void addMember(Player owner, String targetName) {
        RegionManager regions = getRegionManager(owner.getWorld());
        if (regions == null) {
            owner.sendMessage(ChatColor.RED + "A proteção de terrenos não está habilitada neste mundo.");
            return;
        }

        Optional<ProtectedRegion> ownedRegion = findOwnedRegion(owner);
        if (ownedRegion.isEmpty()) {
            owner.sendMessage(ChatColor.RED + "Você não possui um terreno ou não é o dono.");
            return;
        }
        ProtectedRegion region = ownedRegion.get();

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            owner.sendMessage(ChatColor.RED + "Jogador '" + targetName + "' não encontrado.");
            return;
        }

        DefaultDomain members = region.getMembers();
        if (members.contains(target.getUniqueId())) {
            owner.sendMessage(ChatColor.YELLOW + target.getName() + " já é um membro do seu terreno.");
            return;
        }

        members.addPlayer(target.getUniqueId());
        region.setMembers(members);
        owner.sendMessage(ChatColor.GREEN + target.getName() + " foi adicionado ao seu terreno!");
    }

    public void removeMember(Player owner, String targetName) {
        RegionManager regions = getRegionManager(owner.getWorld());
        if (regions == null) {
            owner.sendMessage(ChatColor.RED + "A proteção de terrenos não está habilitada neste mundo.");
            return;
        }

        Optional<ProtectedRegion> ownedRegion = findOwnedRegion(owner);
        if (ownedRegion.isEmpty()) {
            owner.sendMessage(ChatColor.RED + "Você não possui um terreno ou não é o dono.");
            return;
        }
        ProtectedRegion region = ownedRegion.get();

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            owner.sendMessage(ChatColor.RED + "Jogador '" + targetName + "' não encontrado.");
            return;
        }

        DefaultDomain members = region.getMembers();
        if (!members.contains(target.getUniqueId())) {
            owner.sendMessage(ChatColor.YELLOW + target.getName() + " não é um membro do seu terreno.");
            return;
        }

        members.removePlayer(target.getUniqueId());
        region.setMembers(members);
        owner.sendMessage(ChatColor.GREEN + target.getName() + " foi removido do seu terreno.");
    }

    public void abandonClaim(Player player) {
        RegionManager regions = getRegionManager(player.getWorld());
        if (regions == null) {
            player.sendMessage(ChatColor.RED + "A proteção de terrenos não está habilitada neste mundo.");
            return;
        }

        Optional<ProtectedRegion> ownedRegion = findOwnedRegion(player);
        if (ownedRegion.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Você não possui um terreno para abandonar.");
            return;
        }

        String regionId = ownedRegion.get().getId();
        regions.removeRegion(regionId);
        player.sendMessage(ChatColor.GOLD + "Você abandonou seu terreno. A área agora está desprotegida.");
    }

    public void teleportToLand(Player player) {
        // --- Validação de Cooldown ---
        long cooldownSeconds = plugin.getConfig().getLong("land-protection.home-cooldown-seconds", 300);
        if (cooldownSeconds > 0) {
            if (homeCooldowns.containsKey(player.getUniqueId())) {
                long secondsSinceLastUse = (System.currentTimeMillis() - homeCooldowns.get(player.getUniqueId())) / 1000;
                if (secondsSinceLastUse < cooldownSeconds) {
                    long remaining = cooldownSeconds - secondsSinceLastUse;
                    player.sendMessage(ChatColor.RED + "Você precisa esperar mais " + remaining + " segundos para usar este comando novamente.");
                    return;
                }
            }
        }

        Optional<ProtectedRegion> ownedRegionOpt = findOwnedRegion(player);
        if (ownedRegionOpt.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Você não possui um terreno para se teletransportar.");
            return;
        }

        ProtectedRegion region = ownedRegionOpt.get();
        Location teleportLocation;

        // A funcionalidade de home customizado foi desativada temporariamente devido a
        // incompatibilidade com a versão da API do WorldGuard.
        // Location customHome = region.getFlag(Flags.TELEPORT);
        // if (customHome != null) { ... }

            // Se não houver, calcula o centro da região
            World world = player.getWorld();
            BlockVector3 min = region.getMinimumPoint();
            BlockVector3 max = region.getMaximumPoint();
            int centerX = (min.getX() + max.getX()) / 2;
            int centerZ = (min.getZ() + max.getZ()) / 2;

            // Encontra o bloco mais alto e seguro para teletransporte
            teleportLocation = world.getHighestBlockAt(centerX, centerZ).getLocation();
        // }

        // Adiciona um pequeno offset para centralizar o jogador no bloco e evitar sufocamento
        teleportLocation.add(0.5, 1.5, 0.5);
        teleportLocation.setYaw(player.getLocation().getYaw());
        teleportLocation.setPitch(player.getLocation().getPitch());

        player.teleport(teleportLocation);
        player.sendMessage(ChatColor.GREEN + "Você foi teleportado para o seu terreno!");

        // Atualiza o timestamp do cooldown
        if (cooldownSeconds > 0) {
            homeCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    /*
     * DESATIVADO TEMPORARIAMENTE: A flag Flags.TELEPORT não foi encontrada na API do WorldGuard.
     * Esta funcionalidade será reavaliada em uma futura atualização.
     *
    public void setHomePoint(Player player) { ... }
    */

    public void setGreetingMessage(Player player, String message) {
        if (message.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Você precisa fornecer uma mensagem. Ex: /terreno setgreeting Bem-vindo à minha base!");
            return;
        }

        Optional<ProtectedRegion> ownedRegionOpt = findOwnedRegion(player);
        if (ownedRegionOpt.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Você não possui um terreno.");
            return;
        }

        ProtectedRegion region = ownedRegionOpt.get();

        // Formata a mensagem com cores e o nome do jogador
        String formattedMessage = ChatColor.translateAlternateColorCodes('&', message);

        region.setFlag(Flags.GREET_MESSAGE, formattedMessage);
        player.sendMessage(ChatColor.GREEN + "Mensagem de boas-vindas do seu terreno atualizada!");
    }

    public void setFarewellMessage(Player player, String message) {
        if (message.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Você precisa fornecer uma mensagem. Ex: /terreno setfarewell Volte sempre!");
            return;
        }

        Optional<ProtectedRegion> ownedRegionOpt = findOwnedRegion(player);
        if (ownedRegionOpt.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Você não possui um terreno.");
            return;
        }

        ProtectedRegion region = ownedRegionOpt.get();

        // Formata a mensagem com cores
        String formattedMessage = ChatColor.translateAlternateColorCodes('&', message);

        region.setFlag(Flags.FAREWELL_MESSAGE, formattedMessage);
        player.sendMessage(ChatColor.GREEN + "Mensagem de despedida do seu terreno atualizada!");
    }

    public void setRegionFlag(Player player, String flagName, String valueStr) {
        Optional<ProtectedRegion> ownedRegionOpt = findOwnedRegion(player);
        if (ownedRegionOpt.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Você não possui um terreno.");
            return;
        }

        ProtectedRegion region = ownedRegionOpt.get();
        StateFlag flag;

        // Whitelist de flags que o jogador pode alterar
        switch (flagName.toLowerCase()) {
            case "pvp":
                flag = Flags.PVP;
                break;
            case "chest-access":
                flag = Flags.CHEST_ACCESS;
                break;
            default:
                player.sendMessage(ChatColor.RED + "A flag '" + flagName + "' é inválida ou não pode ser alterada.");
                return;
        }

        StateFlag.State state;
        switch (valueStr.toLowerCase()) {
            case "allow":
            case "on":
                state = StateFlag.State.ALLOW;
                break;
            case "deny":
            case "off":
                state = StateFlag.State.DENY;
                break;
            default:
                player.sendMessage(ChatColor.RED + "Valor inválido. Use 'allow' ou 'deny'.");
                return;
        }

        region.setFlag(flag, state);
        player.sendMessage(ChatColor.GREEN + "A permissão '" + flag.getName() + "' do seu terreno foi definida como " + (state == StateFlag.State.ALLOW ? "permitida" : "negada") + ".");
    }

    public void offerLandForSale(Player seller, String buyerName, String priceStr) {
        Optional<ProtectedRegion> ownedRegionOpt = findOwnedRegion(seller);
        if (ownedRegionOpt.isEmpty()) {
            seller.sendMessage(ChatColor.RED + "Você não possui um terreno para vender.");
            return;
        }

        Player buyer = Bukkit.getPlayerExact(buyerName);
        if (buyer == null || !buyer.isOnline()) {
            seller.sendMessage(ChatColor.RED + "O jogador '" + buyerName + "' não está online.");
            return;
        }

        if (seller.getUniqueId().equals(buyer.getUniqueId())) {
            seller.sendMessage(ChatColor.RED + "Você não pode vender um terreno para si mesmo.");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
            if (price < 0) {
                seller.sendMessage(ChatColor.RED + "O preço não pode ser negativo.");
                return;
            }
        } catch (NumberFormatException e) {
            seller.sendMessage(ChatColor.RED + "O preço inserido é inválido.");
            return;
        }

        String regionId = ownedRegionOpt.get().getId();
        SaleOffer offer = new SaleOffer(seller.getUniqueId(), price, regionId);
        saleOffers.put(buyer.getUniqueId(), offer);

        seller.sendMessage(ChatColor.GREEN + "Você ofereceu seu terreno para " + buyer.getName() + " por " + String.format("%,.0f", price) + " Totens.");
        seller.sendMessage(ChatColor.YELLOW + "Aguardando " + buyer.getName() + " aceitar a oferta.");

        buyer.sendMessage(ChatColor.GOLD + "-----------------------------------------");
        buyer.sendMessage(ChatColor.AQUA + seller.getName() + " está vendendo o terreno dele para você por " + String.format("%,.0f", price) + " Totens.");
        buyer.sendMessage(ChatColor.YELLOW + "Use /terreno aceitarvenda " + seller.getName() + " para comprar.");
        buyer.sendMessage(ChatColor.GRAY + "(A oferta expira em 2 minutos)");
        buyer.sendMessage(ChatColor.GOLD + "-----------------------------------------");

        // Remove a oferta após 2 minutos
        new BukkitRunnable() {
            @Override
            public void run() {
                if (saleOffers.remove(buyer.getUniqueId(), offer)) {
                    seller.sendMessage(ChatColor.RED + "Sua oferta de venda para " + buyer.getName() + " expirou.");
                    buyer.sendMessage(ChatColor.RED + "A oferta de compra do terreno de " + seller.getName() + " expirou.");
                }
            }
        }.runTaskLater(plugin, 20L * 120);
    }

    public void acceptSale(Player buyer, String sellerName) {
        SaleOffer offer = saleOffers.get(buyer.getUniqueId());
        OfflinePlayer seller = Bukkit.getOfflinePlayer(sellerName);

        if (offer == null || !offer.seller().equals(seller.getUniqueId())) {
            buyer.sendMessage(ChatColor.RED + "Você não tem uma oferta de venda pendente de " + sellerName + ".");
            return;
        }

        // Validações antes da transferência
        RegionManager regions = getRegionManager(buyer.getWorld());
        if (regions == null) {
            buyer.sendMessage(ChatColor.RED + "O sistema de terrenos não está funcionando. Contate um admin.");
            return;
        }

        ProtectedRegion region = regions.getRegion(offer.regionId());
        if (region == null) {
            buyer.sendMessage(ChatColor.RED + "O terreno que você está tentando comprar não existe mais.");
            saleOffers.remove(buyer.getUniqueId());
            return;
        }

        Economy econ = plugin.getEconomy();
        if (econ.getBalance(buyer) < offer.price()) {
            buyer.sendMessage(ChatColor.RED + "Você não tem Totens suficientes para comprar este terreno.");
            return;
        }

        // Processa a transação
        econ.withdrawPlayer(buyer, offer.price());
        econ.depositPlayer(seller, offer.price());

        // Transfere a propriedade
        DefaultDomain owners = new DefaultDomain();
        owners.addPlayer(buyer.getUniqueId());
        region.setOwners(owners);
        region.setMembers(new DefaultDomain()); // Limpa os membros antigos

        // Renomeia a região para o novo dono
        // A API não permite renomear, então removemos a antiga e criamos uma nova com os mesmos dados
        ProtectedCuboidRegion newRegion = new ProtectedCuboidRegion(
                "temp", region.getMinimumPoint(), region.getMaximumPoint()
        );
        newRegion.setFlags(region.getFlags()); // Copia todas as flags
        newRegion.setOwners(owners);
        String newRegionId = "terreno_" + buyer.getName().toLowerCase() + "_1";
        regions.addRegion(newRegion); // Adiciona a nova região

        saleOffers.remove(buyer.getUniqueId());

        buyer.sendMessage(ChatColor.GREEN + "Parabéns! Você comprou o terreno de " + seller.getName() + "!");
        if (seller.isOnline()) {
            seller.getPlayer().sendMessage(ChatColor.GREEN + "Seu terreno foi vendido para " + buyer.getName() + " por " + String.format("%,.0f", offer.price()) + " Totens!");
        }
    }

    private RegionManager getRegionManager(World world) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        return container.get(BukkitAdapter.adapt(world));
    }

    private Optional<ProtectedRegion> findOwnedRegion(Player player) {
        RegionManager regions = getRegionManager(player.getWorld());
        if (regions == null) return Optional.empty();
        return regions.getRegions().values().stream()
                .filter(r -> r.getOwners().contains(player.getUniqueId()))
                .findFirst();
    }
}