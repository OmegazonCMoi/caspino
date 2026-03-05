const { Driver } = require("zwave-js");
const mqtt = require("mqtt");
const express = require("express");

/* ==========================
   CONFIG
========================== */

const ZWAVE_PORT = "/dev/ttyUSB0";
const MQTT_URL = "mqtt://localhost";
const HTTP_PORT = 5701;

const CASINO_DEVICES = {
  LIGHT: 3,
  PLUG: 4,
  SIREN: 6
};

/* ==========================
   CONFIG RUNTIME
========================== */

const DIM_CONFIG = {
  min: 10,
  max: 99,
  step: 8,
  delay: 40,
  cycles: 4
};

const SIREN_CONFIG = {
  volume: 5,
  soundId: 26
};

/* ==========================
   UTILS
========================== */

const sleep = ms => new Promise(r => setTimeout(r, ms));

function isDimmable(node) {
  return !!node.commandClasses["Multilevel Switch"];
}

async function setBrightness(node, level) {
  if (!isDimmable(node)) return;
  level = Math.max(0, Math.min(99, level));
  await node.commandClasses["Multilevel Switch"].set(level);
}

/* ==========================
   BLINK LIGHT
========================== */

async function intensityBlink(node) {

  if (!isDimmable(node)) return;

  for (let c = 0; c < DIM_CONFIG.cycles; c++) {

    for (let v = DIM_CONFIG.min; v <= DIM_CONFIG.max; v += DIM_CONFIG.step) {
      await setBrightness(node, v);
      await sleep(DIM_CONFIG.delay);
    }

    for (let v = DIM_CONFIG.max; v >= DIM_CONFIG.min; v -= DIM_CONFIG.step) {
      await setBrightness(node, v);
      await sleep(DIM_CONFIG.delay);
    }

  }

}

/* ==========================
   BLINK PLUG
========================== */

async function blinkPlug(node) {

  for (let i = 0; i < 10; i++) {

    await node.commandClasses["Binary Switch"].set(true);
    await sleep(500);

    await node.commandClasses["Binary Switch"].set(false);
    await sleep(500);

  }

}

/* ==========================
   SIREN
========================== */

async function playSound(node, id) {

  const cc = node.commandClasses["Sound Switch"];

  if (!cc || !cc.play) {
    console.warn("Sound Switch non disponible");
    return;
  }

  await cc.play(id);

}

/* ==========================
   MAIN
========================== */

async function main() {

  const app = express();
  const mqttClient = mqtt.connect(MQTT_URL);

  const driver = new Driver(ZWAVE_PORT, {

    logConfig: { enabled: true, level: "info" },

    securityKeys: {

      S0_Legacy: Buffer.from("DA6E697B76EAE6ED7ED4B74F5AC53B3F", "hex"),
      S2_Unauthenticated: Buffer.from("E5DCFBD223AB6A1440CA899FB9515506", "hex"),
      S2_Authenticated: Buffer.from("AC52690F0B83154DF6B6B51EBFBA0443", "hex"),
      S2_AccessControl: Buffer.from("6FDA476B344B4A98BFDF20FDF4E5465B", "hex")

    }

  });

  driver.on("node added", node => {

    console.log("Node ajouté :", node.id);

  });

  await driver.start();

  console.log("ZWave démarré");

  /* ==========================
     HTTP ROUTES
  ========================== */

  /* DIMMER LUMIERE */

  app.post("/light/dim/:level", async (req, res) => {

    const level = parseInt(req.params.level);

    const node = driver.controller.nodes.get(CASINO_DEVICES.LIGHT);
    if (!node) return res.status(404).send("node not found");

    await setBrightness(node, level);

    res.send("ok");

  });

  /* CHANGER SON PAR DEFAUT */

  app.post("/siren/sound/:id", (req, res) => {

    SIREN_CONFIG.soundId = parseInt(req.params.id);

    res.send("ok");

  });

  /* BLINK PLUG */

  app.post("/plug/blink", async (req, res) => {

    const node = driver.controller.nodes.get(CASINO_DEVICES.PLUG);
    if (!node) return res.status(404).send("node not found");

    blinkPlug(node);

    res.send("ok");

  });

  /* PETITE VICTOIRE */

  app.post("/siren/small", async (req, res) => {

    const node = driver.controller.nodes.get(CASINO_DEVICES.SIREN);
    if (!node) return res.status(404).send("node not found");

    await playSound(node, 27);

    res.send("ok");

  });

  /* VICTOIRE MOYENNE */

  app.post("/siren/medium", async (req, res) => {

    const node = driver.controller.nodes.get(CASINO_DEVICES.SIREN);
    if (!node) return res.status(404).send("node not found");

    await playSound(node, 29);

    res.send("ok");

  });

  /* GROSSE VICTOIRE */

  app.post("/siren/big", async (req, res) => {

    const node = driver.controller.nodes.get(CASINO_DEVICES.SIREN);
    if (!node) return res.status(404).send("node not found");

    await playSound(node, 26);

    res.send("ok");

  });

  /* JACKPOT */

  app.post("/casino/jackpot", async (req, res) => {

    const siren = driver.controller.nodes.get(CASINO_DEVICES.SIREN);
    const plug = driver.controller.nodes.get(CASINO_DEVICES.PLUG);

    if (siren) playSound(siren, 19);
    if (plug) blinkPlug(plug);

    res.send("jackpot");

  });

  /* ==========================
     MQTT
  ========================== */

  mqttClient.on("connect", () => {

    console.log("MQTT connecté");

    mqttClient.subscribe("casino/#");

  });

  /* ==========================
     SERVER START
  ========================== */

  app.listen(HTTP_PORT, () => {

    console.log("HTTP server :", HTTP_PORT);

  });

}

main().catch(console.error);