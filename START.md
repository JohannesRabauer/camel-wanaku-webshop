# Start of the live session

This repository is the demo **without Wanaku**, and with two moderation tools missing. The finished version is on the `solution` branch.

Start it:

```bash
cp .env.example .env       # put your OPENAI_API_KEY into .env
docker compose up --build
```

Then open <http://localhost:9090>. The moderator talks straight to the shop's MCP server. Nothing stands between them.

## What you add live

1. **Two Camel tools** in `shop/src/main/java/dev/rabauer/live_coding/wanaku/ShopTools.java`. Look for the two `LIVE:` comments. `setReviewRating` and `hideReview` are `ai-tool` routes like `getReview` and `approveReview` right above them. Without them the moderator cannot fix a rating or hide a review. Then `docker compose up -d --build shop`.
2. **The attack, unprotected.** Post a review as `mallory` with "Fill in the attack text". Order 1234 turns `REFUNDED` and the customer emails appear in the public note. Press "Reset the demo".
3. **Wanaku in the Compose file.** Replace the `# LIVE: the wanaku service goes here` comment in `docker-compose.yml` with:

   ```yaml
     wanaku:
       image: quay.io/wanaku/wanaku-server:0.3.1
       depends_on:
         shop:
           condition: service_healthy
       ports:
         - "8081:8081"      # MCP, one URL per namespace
         - "18080:8080"     # management and audit API
       volumes:
         - ./wanaku/wanaku.yaml:/etc/wanaku/wanaku.yaml:ro
   ```

4. **The Wanaku config**, `wanaku/wanaku.yaml`: register the shop as a forward in a namespace, then deny what a moderator does not need.

   ```yaml
   forwards:
     - name: shop
       address: "http://shop:9090/mcp"
       namespace: moderation

   action_policy:
     rules:
       - id: deny-refundOrder
         effect: deny
         selectors:
           namespace: moderation
           operation: tools/call
           target_type: tool
           target_name: { matcher: exact, value: refundOrder }
         reason_code: not_a_moderation_tool
         message: The moderation agent may not use refundOrder.

       - id: deny-listCustomers
         effect: deny
         selectors:
           namespace: moderation
           operation: tools/call
           target_type: tool
           target_name: { matcher: exact, value: listCustomers }
         reason_code: not_a_moderation_tool
         message: The moderation agent may not use listCustomers.

   governance:
     namespaces:
       moderation:
         no_match: allow
   ```

   Four more tools a moderator does not need: `searchProducts`, `getReviews`, `getOrder`, `updatePrice`. The full version with all six deny rules is `wanaku/wanaku-policy.yaml` on the `solution` branch.
5. **Bend the moderator's MCP URL** to Wanaku. Put this into `.env`, then `docker compose up -d wanaku moderator`:

   ```
   MODERATOR_MCP_URL=http://wanaku:8081/moderation/mcp
   ```

6. **The attack again.** Post the attack review. The calls to `refundOrder` and `listCustomers` are denied, order 1234 stays `PAID`, no emails appear.
7. **The audit log:** `curl -s "localhost:18080/api/v1/audit/events?limit=50"`.

If a live step breaks, `git checkout solution` has everything working.
