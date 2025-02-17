Architettura:
<pre>
                kubernetes:1.28
	k8s master:
		ip: 192.168.17.97
		user: elisa
		hostname: edrudi-k3s-master
		os: ubuntu-22.04
    init: systemd
		cpu: 8 core
		RAM: 8 GB
		disco: 80GB
	k8s worker:
		ip: 192.168.17.95
		user: elisa
		hostname: edrudi-k3s-worker
		os: ubuntu-22.04
    init: systemd
		cpu: 8 core
		RAM: 8 GB
		disco: 80GB
</pre>

### AZIONI DA ESEGUIRE SUL NODO "SERVER"  (ossia nodo che fa da control plane)

# 0) update /etc/hosts with name for our 
# 1) install K3s and all its dependencies
`curl -sfL https://get.k3s.io | sh - `
# 2) retrieve the K3S_TOKEN
`sudo cat /var/lib/rancher/k3s/server/node-token`


### AZIONI DA ESEGUIRE SUL NODO "AGENT"  (ossia nodo worker)

# 1) aggiungi il nodo al cluster creato dal nodo master
curl -sfL https://get.k3s.io | K3S_URL=https://192.168.17.97:6443 K3S_TOKEN=K1017ef4d5281e981e47111d642171235c256c5a0909734f3dfcbd8a974e9bbd72e::server:45ba75ea4ca7f4b22a118c3782b37f2a sh -
